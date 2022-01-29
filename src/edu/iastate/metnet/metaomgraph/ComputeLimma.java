package edu.iastate.metnet.metaomgraph;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.renjin.primitives.matrix.DoubleMatrixBuilder;
import org.renjin.primitives.matrix.StringMatrixBuilder;
import org.renjin.script.*;

public class ComputeLimma {

    private final String selectedList;
    private final HashMap<String, String> groups;
    private final MetaOmProject myProject;
    private final ArrayList<Integer> limmaCountsInd;

    public ComputeLimma(String selectedList, HashMap<String, String> groups, MetaOmProject myProject,  ArrayList<Integer> limmaCountsInd) {
        this.selectedList = selectedList;
        this.groups = groups;
        this.myProject = myProject;
        this.limmaCountsInd = limmaCountsInd;
    }

    public void calc() throws ScriptException, IOException, URISyntaxException {
        int[] selected = myProject.getGeneListRowNumbers(this.selectedList);
        double[][] thisData;
        String[] rowHeaders;
        String[] columnHeaders;

        StringMatrixBuilder rGroups = new StringMatrixBuilder(2, limmaCountsInd.size());

        // create a script engine manager:
        RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
        // create a Renjin engine:
        ScriptEngine engine = factory.getScriptEngine();

        int[] countsInd = limmaCountsInd.stream().mapToInt(i->i).toArray();

        // Start of Limma analysis
        thisData = myProject.getSelectedListOnlyRowData(countsInd, selectedList);

        rowHeaders = myProject.getDefaultRowNames(selected);
        columnHeaders = myProject.getDataColumnHeaders(countsInd);

        DoubleMatrixBuilder rData = new DoubleMatrixBuilder(selected.length, limmaCountsInd.size());
        for (int row = 0; row < selected.length; row++) {
            for (int col = 0; col < limmaCountsInd.size(); col++) {
                rData.setValue(row, col, thisData[col][row]);
            }
        }
        rData.setColNames(Arrays.asList(columnHeaders));
        rData.setRowNames(Arrays.asList(rowHeaders));

        for (int i = 0; i < limmaCountsInd.size(); i++) {
            rGroups.setValue(0, i, columnHeaders[i]);
            rGroups.setValue(1, i, groups.get(columnHeaders[i]));
        }
        rGroups.setRowNames(Arrays.asList("Samples", "Groups"));

        // Preprocessing
        engine.eval("library(edgeR)");

        engine.put("counts", rData.build());

        // CONVERTING THE COUNT MATRIX TO A DGELIST OBJECT AND CALCULATING THE NORMALIZATION FACTORS

        engine.eval("d <- DGEList(counts)");
        engine.eval("d <- calcNormFactors(d)");

        // READING GROUPS AND TODO PLOTTING MDS CHART

        String homePath = System.getProperty("user.home");
        engine.eval("snames <- colnames(counts)");
        engine.put("groups", rGroups.build());
        engine.eval("group2 <- interaction(groups[2,])");
        engine.eval("png()");
        String mdsPath = "png(\"" + homePath + "/metaomgraph/mds.png\")";
        engine.eval(mdsPath);
        mdsPlot(factory, engine);
        engine.eval("plotMDSv2(d, col = as.numeric(group2))");
        engine.eval("dev.off()");

        // PERFORMING THE LIMMA VOOM OPERATION
        String voomPath = "png(\"" + homePath + "/metaomgraph/voom.png\")";
        engine.eval("mm <- model.matrix(~0 + group2)");
        engine.eval(voomPath);
        engine.eval("y <- voom(d, mm, plot = T)");
        engine.eval("dev.off()");

        // Finding differentially expressed genes
        engine.eval("fit <- lmFit(y, mm)");
        engine.eval("fitbit <- colnames(coef(fit))");

        engine.eval("contr <- makeContrasts(group21 - group22, levels = colnames(coef(fit)))");

        engine.eval("tmp <- contrasts.fit(fit, contr)");
        engine.eval("tmp <- eBayes(tmp)");

        engine.eval("top.table <- topTable(tmp, coef=1, sort.by = \"P\", n = Inf)");
        engine.eval("top.table$Gene <- rownames(top.table)");
        engine.eval("top.table <- top.table[,c(\"Gene\", names(top.table)[1:6])]");

        String writeDETable = "write.table(top.table, file = \"" + homePath + "/metaomgraph/differentialexpression.tsv" + "\", row.names = F, sep = \"\t\", quote = F)";
        engine.eval(writeDETable);

//            StringVector genes = (StringVector)engine.eval("top.table$Gene");
//            DoubleVector logFC = (DoubleVector)engine.eval("top.table$logFC");
//            DoubleVector aveExpr = (DoubleVector)engine.eval("top.table$AveExpr");
//            DoubleVector t = (DoubleVector)engine.eval("top.table$t");
//            DoubleVector pVal = (DoubleVector)engine.eval("top.table$P.Value");
//            DoubleVector adjPVal = (DoubleVector)engine.eval("top.table$adj.P.Val");
    }

    private void mdsPlot(RenjinScriptEngineFactory factory, ScriptEngine engine) throws ScriptException {
        engine.eval("#  File src/library/stats/R/cmdscale.R\n" +
                "#  Part of the R package, https://www.R-project.org\n" +
                "#\n" +
                "#  Copyright (C) 1995-2015 The R Core Team\n" +
                "#\n" +
                "#  This program is free software; you can redistribute it and/or modify\n" +
                "#  it under the terms of the GNU General Public License as published by\n" +
                "#  the Free Software Foundation; either version 2 of the License, or\n" +
                "#  (at your option) any later version.\n" +
                "#\n" +
                "#  This program is distributed in the hope that it will be useful,\n" +
                "#  but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                "#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
                "#  GNU General Public License for more details.\n" +
                "#\n" +
                "#  A copy of the GNU General Public License is available at\n" +
                "#  https://www.R-project.org/Licenses/\n" +
                "\n" +
                "cmdscalev2 <- function (d, k = 2, eig = FALSE, add = FALSE, x.ret = FALSE,\n" +
                "\t\t      list. = eig || add || x.ret)\n" +
                "{\n" +
                "    if (anyNA(d))\n" +
                "\tstop(\"NA values not allowed in 'd'\")\n" +
                "    if(!list.) {\n" +
                "\tif (eig)  warning(  \"eig=TRUE is disregarded when list.=FALSE\")\n" +
                "\tif(x.ret) warning(\"x.ret=TRUE is disregarded when list.=FALSE\")\n" +
                "    }\n" +
                "    if (is.null(n <- attr(d, \"Size\"))) {\n" +
                "        if(add) d <- as.matrix(d)\n" +
                "\tx <- as.matrix(d^2)\n" +
                "        storage.mode(x) <- \"double\"\n" +
                "\tif ((n <- nrow(x)) != ncol(x))\n" +
                "\t    stop(\"distances must be result of 'dist' or a square matrix\")\n" +
                "        rn <- rownames(x)\n" +
                "    } else { # d is  dist()-like  object\n" +
                "        rn <- attr(d, \"Labels\")\n" +
                "\tx <- matrix(0, n, n) # must be double\n" +
                "        if (add) d0 <- x\n" +
                "\tx[row(x) > col(x)] <- d^2\n" +
                "\tx <- x + t(x)\n" +
                "        if (add) {\n" +
                "            d0[row(x) > col(x)] <- d\n" +
                "            d <- d0 + t(d0)\n" +
                "        }\n" +
                "    }\n" +
                "    n <- as.integer(n)\n" +
                "    ## we need to handle nxn internally in dblcen\n" +
                "    if(is.na(n) || n > 46340) stop(\"invalid value of 'n'\")\n" +
                "    if((k <- as.integer(k)) > n - 1 || k < 1)\n" +
                "        stop(\"'k' must be in {1, 2, ..  n - 1}\")\n" +
                "    ## NB: this alters argument x, which is OK as it is re-assigned.\n" +
                "    x <- scale(t(scale(t(x), scale=FALSE)),scale=FALSE)\n" +
                "\n" +
                "    if(add) { ## solve the additive constant problem\n" +
                "        ## it is c* = largest eigenvalue of 2 x 2 (n x n) block matrix Z:\n" +
                "        i2 <- n + (i <- 1L:n)\n" +
                "        Z <- matrix(0, 2L*n, 2L*n)\n" +
                "        Z[cbind(i2,i)] <- -1\n" +
                "        Z[ i, i2] <- -x\n" +
                "        Z[i2, i2] <- scale(t(scale(t(2*d), scale=FALSE)),scale=FALSE)\n" +
                "        e <- eigen(Z, symmetric = FALSE, only.values = TRUE)$values\n" +
                "        add.c <- max(Re(e))\n" +
                "        ## and construct a new x[,] matrix:\n" +
                "\tx <- matrix(double(n*n), n, n)\n" +
                "        non.diag <- row(d) != col(d)\n" +
                "        x[non.diag] <- (d[non.diag] + add.c)^2\n" +
                "        x <- scale(t(scale(t(x), scale=FALSE)),scale=FALSE)\n" +
                "    }\n" +
                "    e <- eigen(-x/2, symmetric = TRUE)\n" +
                "    ev <- e$values[seq_len(k)]\n" +
                "    evec <- e$vectors[, seq_len(k), drop = FALSE]\n" +
                "    k1 <- sum(ev > 0)\n" +
                "    if(k1 < k) {\n" +
                "        warning(gettextf(\"only %d of the first %d eigenvalues are > 0\", k1, k),\n" +
                "                domain = NA)\n" +
                "        evec <- evec[, ev > 0,  drop = FALSE]\n" +
                "        ev <- ev[ev > 0]\n" +
                "    }\n" +
                "    points <- evec * rep(sqrt(ev), each=n)\n" +
                "    dimnames(points) <- list(rn, NULL)\n" +
                "    if (list.) {\n" +
                "        evalus <- e$values # Cox & Cox have sum up to n-1, though\n" +
                "        list(points = points, eig = if(eig) evalus, x = if(x.ret) x,\n" +
                "             ac = if(add) add.c else 0,\n" +
                "             GOF = sum(ev)/c(sum(abs(evalus)), sum(pmax(evalus, 0))) )\n" +
                "    } else points\n" +
                "}");

        engine.eval("##  PLOTMDS.R\n" +
                "\n" +
                "#\tClass to hold multidimensional scaling output\n" +
                "setClass(\"MDS\",representation(\"list\"))\n" +
                "\n" +
                "setMethod(\"show\",\"MDS\",function(object) {\n" +
                "\tcat(\"An object of class MDS\\n\")\n" +
                "\tprint(unclass(object))\n" +
                "})\n" +
                "\n" +
                "plotMDSv2 <- function(x,...) UseMethod(\"plotMDSv2\")\n" +
                "\n" +
                "plotMDSv2.MDS <- function(x,labels=NULL,pch=NULL,cex=1,dim.plot=NULL,xlab=NULL,ylab=NULL,...)\n" +
                "#\tMethod for MDS objects\n" +
                "#\tCreate a new plot using MDS coordinates or distances previously created\n" +
                "#\tGordon Smyth and Yifang Hu\n" +
                "#\t21 May 2011.  Last modified 29 December 2014\n" +
                "{\n" +
                "#\tCheck labels\n" +
                "\tif(is.null(labels) & is.null(pch)) {\n" +
                "\t\tlabels <- colnames(x$distance.matrix)\n" +
                "\t\tif(is.null(labels)) labels <- 1:length(x$x)\n" +
                "\t}\n" +
                "\n" +
                "#\tAre new dimensions requested?\n" +
                "\tif(is.null(dim.plot)) {\n" +
                "\t\tdim.plot <- x$dim.plot\n" +
                "\t} else {\n" +
                "\t\tif(any(dim.plot != x$dim.plot)) {\n" +
                "\t\t\tndim <- max(dim.plot)\n" +
                "\t\t\tif(ndim > ncol(x$cmdscale.out)) x$cmdscale.out <- cmdscalev2(as.dist(x$distance.matrix),k=ndim)\n" +
                "\t\t\tx$x <- x$cmdscale.out[,dim.plot[1]]\n" +
                "\t\t\tx$y <- x$cmdscale.out[,dim.plot[2]]\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\n" +
                "#\tAxis labels\n" +
                "\tif(is.null(x$axislabel)) x$axislabel <- \"Principal Coordinate\"\n" +
                "\tif(is.null(xlab)) xlab <- paste(x$axislabel,dim.plot[1])\n" +
                "\tif(is.null(ylab)) ylab <- paste(x$axislabel,dim.plot[2])\n" +
                "\n" +
                "#\tMake the plot\n" +
                "\tif(is.null(labels)){\n" +
                "#\t\tPlot symbols instead of text\n" +
                "\t\tplot(x$x, x$y, pch = pch, xlab = xlab, ylab = ylab, cex = cex, ...)\n" +
                "\t} else {\n" +
                "#\t\tPlot text.\n" +
                "\t\tlabels <- as.character(labels)\n" +
                "#\t\tNeed to estimate width of labels in plot coordinates.\n" +
                "#\t\tEstimate will be ok for default plot width, but maybe too small for smaller plots.\n" +
                "\t\tStringRadius <- 0.01*cex*nchar(labels)\n" +
                "\t\tleft.x <- x$x-StringRadius\n" +
                "\t\tright.x <- x$x+StringRadius\n" +
                "\t\tplot(c(left.x, right.x), c(x$y, x$y), type = \"n\", xlab = xlab, ylab = ylab, ...)\n" +
                "\t\ttext(x$x, x$y, labels = labels, cex = cex, ...)\n" +
                "\t}\n" +
                "\n" +
                "\tinvisible(x)\n" +
                "}\n" +
                "\n" +
                "plotMDSv2.default <- function(x,top=500,labels=NULL,pch=NULL,cex=1,dim.plot=c(1,2),ndim=max(dim.plot),gene.selection=\"pairwise\",xlab=NULL,ylab=NULL,plot=TRUE,...)\n" +
                "#\tMulti-dimensional scaling with top-distance\n" +
                "#\tDi Wu and Gordon Smyth\n" +
                "#\t19 March 2009.  Last modified 6 Oct 2016\n" +
                "{\n" +
                "#\tCheck x\n" +
                "\tx <- as.matrix(x)\n" +
                "\tnsamples <- ncol(x)\n" +
                "\tif(nsamples < 3) stop(paste(\"Only\",nsamples,\"columns of data: need at least 3\"))\n" +
                "\tcn <- colnames(x)\n" +
                "#\tRemove rows with missing or Inf values\n" +
                "\tbad <- rowSums(is.finite(x)) < nsamples\n" +
                "\tif(any(bad)) x <- x[!bad,,drop=FALSE]\n" +
                "\tnprobes <- nrow(x)\n" +
                "\n" +
                "#\tCheck top\n" +
                "\ttop <- min(top,nprobes)\n" +
                "\n" +
                "#\tCheck labels and pch\n" +
                "\tif(is.null(pch) & is.null(labels)) {\n" +
                "\t\tlabels <- colnames(x)\n" +
                "\t\tif(is.null(labels)) labels <- 1:nsamples\n" +
                "\t}\n" +
                "\tif(!is.null(labels)) labels <- as.character(labels)\n" +
                "\n" +
                "#\tCheck dim.plot\n" +
                "\tdim.plot <- unique(as.integer(dim.plot))\n" +
                "\tif(length(dim.plot) != 2L) stop(\"dim.plot must specify two dimensions to plot\")\n" +
                "\n" +
                "#\tCheck dim\n" +
                "\tif(ndim < 2L) stop(\"Need at least two dim.plot\")\n" +
                "\tif(nsamples < ndim) stop(\"ndim is greater than number of samples\")\n" +
                "\tif(nprobes < ndim) stop(\"ndim is greater than number of rows of data\")\n" +
                "\n" +
                "#\tCheck gene.selection\n" +
                "\tgene.selection <- match.arg(gene.selection,c(\"pairwise\",\"common\"))\n" +
                "\n" +
                "#\tDistance matrix from pairwise leading fold changes\n" +
                "\tdd <- matrix(0,nrow=nsamples,ncol=nsamples,dimnames=list(cn,cn))\n" +
                "\tif(gene.selection==\"pairwise\") {\n" +
                "#\t\tDistance measure is mean of top squared deviations for each pair of arrays\n" +
                "\t\ttopindex <- nprobes-top+1L\n" +
                "\t\tfor (i in 2L:(nsamples))\n" +
                "\t\tfor (j in 1L:(i-1L))\n" +
                "\t\t\tdd[i,j]=sqrt(mean(sort.int((x[,i]-x[,j])^2,partial=topindex)[topindex:nprobes]))\n" +
                "\t\taxislabel <- \"Leading logFC dim\"\n" +
                "\t} else {\n" +
                "#\t\tSame genes used for all comparisons\n" +
                "\t\tif(nprobes > top) {\n" +
                "\t\t\ts <- rowMeans((x-rowMeans(x))^2)\n" +
                "\t\t\to <- order(s,decreasing=TRUE)\n" +
                "\t\t\tx <- x[o[1L:top],,drop=FALSE]\n" +
                "\t\t}\n" +
                "\t\tfor (i in 2L:(nsamples))\n" +
                "\t\t\tdd[i,1L:(i-1L)]=sqrt(colMeans((x[,i]-x[,1:(i-1),drop=FALSE])^2))\n" +
                "\t\taxislabel <- \"Principal Component\"\n" +
                "\t}\n" +
                "\n" +
                "#\tMulti-dimensional scaling\n" +
                "\ta1 <- suppressWarnings(cmdscalev2(as.dist(dd),k=ndim))\n" +
                "\n" +
                "#\tMake MDS object and call plotMDS method\n" +
                "\tmds <- new(\"MDS\",list(dim.plot=dim.plot,distance.matrix=dd,cmdscale.out=a1,top=top,gene.selection=gene.selection))\n" +
                "\tif(dim.plot[1] > ncol(a1)) {\n" +
                "\t\tmds$x <- rep_len(0,length.out=nsamples)\n" +
                "\t\twarning(paste(\"dimension\",dim.plot[1],\"is degenerate or all zero\"))\n" +
                "\t} else\n" +
                "\t\tmds$x <- a1[,dim.plot[1]]\n" +
                "\tif(dim.plot[2] > ncol(a1)) {\n" +
                "\t\tmds$y <- rep_len(0,length.out=nsamples)\n" +
                "\t\twarning(paste(\"dimension\",dim.plot[2],\"is degenerate or all zero\"))\n" +
                "\t} else\n" +
                "\t\tmds$y <- a1[,dim.plot[2]]\n" +
                "\tmds$top <- top\n" +
                "\tmds$axislabel <- axislabel\n" +
                "\tif(plot)\n" +
                "\t\tplotMDSv2(mds,labels=labels,pch=pch,cex=cex,xlab=xlab,ylab=ylab,...)\n" +
                "\telse\n" +
                "\t\tmds\n" +
                "}");
    }
}
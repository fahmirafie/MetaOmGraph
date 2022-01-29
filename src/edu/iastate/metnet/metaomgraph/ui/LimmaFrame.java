package edu.iastate.metnet.metaomgraph.ui;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import edu.iastate.metnet.metaomgraph.*;
import edu.iastate.metnet.metaomgraph.Metadata.MetadataQuery;
import edu.iastate.metnet.metaomgraph.logging.ActionProperties;

import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ActionEvent;

/**
 *
 * UI class to choose which features to use in the Limma Analysis
 *
 */

public class LimmaFrame extends TaskbarInternalFrame implements ActionListener {

    private JComboBox comboBox;

    private JPanel groupPanel;

    JLabel lblN2;
    JLabel lblN1;

    private JTextField txtGroup1;
    private JTextField txtGroup2;

    private JScrollPane jscp1;
    private JTable tableGrp1;

    private JScrollPane jscp2;
    private JTable tableGrp2;

    ArrayList<LimmaGroupPanel> limmaGroupPanels;

    private MetadataHybrid mdob;
    private MetaOmProject myProject;

    private boolean[] excludedCopy;

    /**
     * Default Properties
     */

    private Color SELECTIONBCKGRND = MetaOmGraph.getTableSelectionColor();
    private Color BCKGRNDCOLOR1 = MetaOmGraph.getTableColorEven();
    private Color BCKGRNDCOLOR2 = MetaOmGraph.getTableColorOdd();
    private Color HIGHLIGHTCOLOR = MetaOmGraph.getTableHighlightColor();
    private Color HYPERLINKCOLOR = MetaOmGraph.getTableHyperlinkColor();

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    LimmaFrame frame = new LimmaFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public LimmaFrame() {
        setBounds(100, 100, 750, 300);
        getContentPane().setLayout(new BorderLayout(0, 0));
        setTitle("Limma Analysis");

        // init objects
        myProject = MetaOmGraph.getActiveProject();
        mdob = myProject.getMetadataHybrid();
        limmaGroupPanels = new ArrayList<>();
        if (mdob == null) {
            JOptionPane.showMessageDialog(null, "Error. No metadata found", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }

        // get excluded
        boolean[] excluded = MetaOmAnalyzer.getExclude();
        if (excluded != null) {
            excludedCopy = new boolean[excluded.length];
            System.arraycopy(excluded, 0, excludedCopy, 0, excluded.length);
        }

        groupPanel = new JPanel();
        groupPanel.setLayout(new BoxLayout(groupPanel, 0));
        getContentPane().add(groupPanel);

        initComboBoxes();
        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.NORTH);
        JLabel lblTop = new JLabel("Select feature list");
        panel.add(lblTop);
        panel.add(comboBox);

        JScrollPane scrollPane = new JScrollPane(groupPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane);

        JPanel panel_1 = new JPanel();
        getContentPane().add(panel_1, BorderLayout.SOUTH);

        JButton btnOk = new JButton("Ok");
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String errorMsg = "";
                boolean shouldExit = false;
                int totalSize = 0;
                for (LimmaGroupPanel group : limmaGroupPanels) {
                    List<String> grp = group.getAllRows();
                    if (grp == null || grp.size() < 1) {
                        errorMsg += "Group "+ group.getId() +" is empty\n";
                        shouldExit = true;
                    } else if (grp.size() > 100) {
                        errorMsg += "Group size of < 100 recommended for group "+group.getId()+"\n";
                        totalSize += grp.size();
                    }
                }
                if (errorMsg != "") {
                    if (totalSize > 1000) {
                        errorMsg += "Overall size of < 1000 is recommended for Limma Analysis\n";
                    }
                    if (shouldExit) {
                        JOptionPane.showMessageDialog(null, errorMsg);
                        return;
                    } else {
                        errorMsg += "\nContinue anyway?";
                        int stopRequested = JOptionPane.showConfirmDialog(null, errorMsg);
                        if (stopRequested != 0) {
                            return;
                        }
                    }
                }

                //Check if lists are disjoint
                HashMap<String, String> map = new HashMap<>();
                ArrayList<String> limmaCounts = new ArrayList<>();
                ArrayList<Integer> limmaGroups = new ArrayList<>();
                ArrayList<Integer> limmaCountsInd = new ArrayList<>();

                for (LimmaGroupPanel group : limmaGroupPanels) {
                    for (String entry : group.getAllRows()) {
                        if (map.get(entry) == null) { // if null we've never seen that name
                            map.put(entry, String.valueOf(group.getId()));
                            limmaCounts.add(entry);
                            limmaGroups.add(group.getId());
                        } else { // if not null that name exists somewhere else immediately cancel
                            JOptionPane.showMessageDialog(null, "The two groups must be disjoint. Please check the lists",
                                    "Please check the lists", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }

                limmaCountsInd = getIndices(limmaCounts);

                String selectedFeatureList = comboBox.getSelectedItem().toString();
                ComputeLimma ob = new ComputeLimma(selectedFeatureList, map, myProject, limmaCountsInd);

                // save object
                String id = "";

                final String id_f = id;
                //cant'start with string

                new AnimatedSwingWorker("Working...", true) {
                    @Override
                    public Object construct() {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                // Start calculating
                                try {
                                    ob.calc();
                                    JOptionPane.showMessageDialog(panel, "Limma analysis results saved at " + System.getProperty("user.home") + "/metaomgraph/differentialexpression.tsv\n"
                                            + "MDS plot saved at " + System.getProperty("user.home") + "/metaomgraph/mds.png\"\n"
                                            + "Voom plot saved at " + System.getProperty("user.home") + "/metaomgraph/voom.png\"\n"
                                    );
                                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                        Desktop.getDesktop().browse(new URI("file://" + System.getProperty("user.home") + "/metaomgraph/voom.png"));
                                        Desktop.getDesktop().browse(new URI("file://" + System.getProperty("user.home") + "/metaomgraph/mds.png"));
                                    }
                                } catch (Exception e) {
                                    StringWriter sw = new StringWriter();
                                    PrintWriter pw = new PrintWriter(sw);
                                    e.printStackTrace(pw);
                                    String sStackTrace = sw.toString();

                                    JDialog jd = new JDialog();
                                    JTextPane jt = new JTextPane();
                                    jt.setText(sStackTrace);
                                    jt.setBounds(10, 10, 300, 100);
                                    jd.getContentPane().add(jt);
                                    jd.setBounds(100, 100, 500, 200);
                                    jd.setVisible(true);
                                }
                            }
                        });
                        return null;
                    }
                }.start();


                //Harsha - reproducibility log

                HashMap<String,Object> actionMap = new HashMap<String,Object>();
                HashMap<String,Object> dataMap = new HashMap<String,Object>();
                HashMap<String,Object> result = new HashMap<String,Object>();

                try {

                    actionMap.put("parent",MetaOmGraph.getCurrentProjectActionId());
                    actionMap.put("section", "All");

                    dataMap.put("Group 1 Name", txtGroup1.getText());
                    dataMap.put("Group 2 Name", txtGroup2.getText());
//                    dataMap.put("Group 1 List", grp1);
//                    dataMap.put("Group 2 List", grp2);
                    dataMap.put("Analysis Name", id);

                    result.put("result", "OK");

                    ActionProperties limmaAction = new ActionProperties("limma-analysis",actionMap,dataMap,result,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS zzz").format(new Date()));
                    limmaAction.logActionProperties();

                }
                catch(Exception e1) {

                }

            }
        });
        panel_1.add(btnOk);

        JButton btnMore = new JButton("More");
        btnMore.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (limmaGroupPanels.size() < 10) {
                    LimmaGroupPanel lgp = new LimmaGroupPanel();
                    lgp.getMoveSelectedBtn().addActionListener(LimmaFrame.this::actionPerformed);
                    limmaGroupPanels.add(lgp);
                    groupPanel.add(lgp);
                    //refresh the window with the new group
                    groupPanel.validate();
                    validate();
                    // if its the first time a user clicks the more button (we just now added our third group)
                    // then expand the window a little to show them where the groups are
                    if (limmaGroupPanels.size() == 3) {
                        setSize(getWidth() + 50, getHeight());
                    }
                }
            }
        });
        panel_1.add(btnMore);

        JButton btnLess = new JButton("Less");
        btnLess.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (limmaGroupPanels.size() > 2) {
                    groupPanel.remove(limmaGroupPanels.size() - 1);
                    limmaGroupPanels.remove(limmaGroupPanels.size() - 1);
                    LimmaGroupPanel.id -= 1;
                    //refresh the window with the new group
                    groupPanel.validate();
                    validate();
                    // if its the first time a user clicks the more button (we just now added our third group)
                    // then expand the window a little to show them where the groups are
                    if (limmaGroupPanels.size() == 2) {
                        setSize(getWidth() - 50, getHeight());
                    }
                }
            }
        });
        panel_1.add(btnLess);

        LimmaGroupPanel.resetId();

        //Start with two panels
        LimmaGroupPanel lgp1 = new LimmaGroupPanel();
        lgp1.getMoveSelectedBtn().addActionListener(this);
        limmaGroupPanels.add(lgp1);
        groupPanel.add(lgp1);
        LimmaGroupPanel lgp2 = new LimmaGroupPanel();
        lgp2.getMoveSelectedBtn().addActionListener(this);
        limmaGroupPanels.add(lgp2);
        groupPanel.add(lgp2);

        // frame properties
        this.setClosable(true);
        pack();

        int defaultWidth = this.getWidth();
        int defaultHeight = MetaOmGraph.getMainWindow().getHeight() / 2;
        LimmaFrame thisFrame = this;

        this.addComponentListener(new ComponentListener() {

            @Override
            public void componentShown(ComponentEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void componentResized(ComponentEvent e) {
                // TODO Auto-generated method stub

                if(thisFrame.getWidth() < defaultWidth || thisFrame.getHeight() < defaultHeight) {
                    thisFrame.setSize(defaultWidth, defaultHeight);
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void componentHidden(ComponentEvent e) {
                // TODO Auto-generated method stub

            }
        });

        putClientProperty("JInternalFrame.frameType", "normal");
        setResizable(true);
        setMaximizable(true);
        setIconifiable(true);
        setClosable(true);

        FrameModel diffExpFrameModel = new FrameModel("DEA","Differential Expression Analysis",12);
        setModel(diffExpFrameModel);
    }

    private JTable initTableModel() {
        JTable table = new JTable() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (!isRowSelected(row)) {
                    c.setBackground(getBackground());
                    int modelRow = convertRowIndexToModel(row);

                    if (row % 2 == 0) {
                        c.setBackground(BCKGRNDCOLOR1);
                    } else {
                        c.setBackground(BCKGRNDCOLOR2);
                    }

                } else {
                    c.setBackground(SELECTIONBCKGRND);
                }

                return c;
            }

        };
        // disable colum drag
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableModel model = new DefaultTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return String.class;
                    default:
                        return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        // set properties
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(true);
        table.setPreferredScrollableViewportSize(table.getPreferredSize());
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Garamond", Font.BOLD, 14));
        return table;
    }

    private void updateTableData(JTable table, List<String> rows) {
        DefaultTableModel tablemodel = (DefaultTableModel) table.getModel();
        tablemodel.setRowCount(0);
        tablemodel.setColumnCount(0);
        // add data
        String dcName = mdob.getDataColName();
        tablemodel.addColumn(dcName);

        if (rows == null) {
            return;
        }

        // convert list to set remove duplicates
        Set<String> tempSet = new TreeSet<String>(rows);
        rows = new ArrayList<>();
        rows.addAll(tempSet);
        Vector temp = null;
        for (String s : rows) {
            temp = new Vector<>();
            temp.add(s);
            tablemodel.addRow(temp);
        }
    }

    private void initComboBoxes() {
        comboBox = new JComboBox(MetaOmGraph.getActiveProject().getGeneListNames());
    }

    /**
     * move selected rows from table 1 to table 2
     */
    private void moveSelectedtoRight() {
        List<String> selected1 = getSelectedRows(tableGrp1);
        addRows(tableGrp2, selected1);
        removeSelectedRows(tableGrp1);
        updateLabelN();
    }

    private void moveSelectedtoLeft() {
        List<String> selected2 = getSelectedRows(tableGrp2);
        addRows(tableGrp1, selected2);
        removeSelectedRows(tableGrp2);
        updateLabelN();

    }

    /**
     * get selected rows from a table
     *
     * @param table
     * @return
     */
    private List<String> getSelectedRows(JTable table) {
        return getSelectedRows(table, false);
    }

    private List<String> getSelectedRows(JTable table, boolean invert) {
        int selected[] = table.getSelectedRows();
        List<String> res = new ArrayList<>();
        for (int i = 0; i < selected.length; i++) {
            String thisRow = "";
            thisRow = (String) table.getValueAt(selected[i], 0);
            res.add(thisRow);
        }
        // JOptionPane.showMessageDialog(null, "sel:" + res);

        if (invert) {
            List<String> temp = new ArrayList<>();
            for (int i = 0; i < table.getRowCount(); i++) {
                String thisRow = "";
                thisRow = (String) table.getValueAt(i, 0);
                if (!res.contains(thisRow)) {
                    temp.add(thisRow);
                }
            }
            res = temp;
        }

        return res;
    }

    private void removeSelectedRows(JTable table) {
        List<String> toKeep = getSelectedRows(table, true);
        // JOptionPane.showMessageDialog(null, "tokeep:" + toKeep.toString());
        updateTableData(table, toKeep);
        updateLabelN();

    }

    private void addRows(JTable table, List<String> toAdd) {
        toAdd.addAll(getAllRows(table));
        // JOptionPane.showMessageDialog(null, "toAdd:" + toAdd.toString());
        updateTableData(table, toAdd);
        updateLabelN();
    }

    private List<String> getAllRows(JTable table) {
        // get existing rows
        List<String> temp = new ArrayList<>();
        for (int i = 0; i < table.getRowCount(); i++) {
            String thisRow = "";
            thisRow = (String) table.getValueAt(i, 0);
            temp.add(thisRow);
        }
        return temp;
    }

    private void updateLabelN() {

        int n1 = getAllRows(tableGrp1).size();
        lblN1.setText("n=" + String.valueOf(n1));
        int n2 = getAllRows(tableGrp2).size();
        lblN2.setText("n=" + String.valueOf(n2));

    }

    /**
     * @author urmi bring the matched items to top and set them as selected
     * @param res
     * @param tab
     */
    private void setSelectedRows(List<String> res, JTable tab) {
        DefaultTableModel model = (DefaultTableModel) tab.getModel();
        List<String> newVals = new ArrayList<>();
        // bring matched values at top
        String temp;
        int total_matches = 0;
        for (int c = 0; c < model.getRowCount(); c++) {
            temp = model.getValueAt(c, 0).toString();
            if (res.contains(temp)) {
                newVals.add("\t:::" + temp);
                total_matches++;
            } else {
                newVals.add("~:::" + temp);
            }
        }
        java.util.Collections.sort(newVals);
        // JOptionPane.showMessageDialog(null, newVals.toString());
        model.setRowCount(0);
        for (int i = 0; i < newVals.size(); i++) {
            Vector<String> v = new Vector<>();
            v.add(newVals.get(i).split(":::")[1]);
            model.addRow(v);

        }
        if (total_matches > 0) {
            tab.setRowSelectionInterval(0, total_matches - 1);
        }
    }

    private List<String> showSearchMetadataPanel() {
        // search datacolumns by metadata and add results
        // display query panel
        try {
            final TreeSearchQueryConstructionPanel tsp = new TreeSearchQueryConstructionPanel(myProject, false);
            final MetadataQuery[] queries;
            queries = tsp.showSearchDialog();
            if (tsp.getQueryCount() <= 0) {
                // System.out.println("Search dialog cancelled");
                // User didn't enter any queries
                return null;
            }
            // final int[] result = new int[myProject.getDataColumnCount()];
            Collection<Integer> result = new ArrayList<>();
            new AnimatedSwingWorker("Searching...", true) {
                @Override
                public Object construct() {
                    try {
                        ArrayList<Integer> toAdd = new ArrayList<Integer>();
                        for (int i = 0; i < myProject.getDataColumnCount(); i++) {
                            toAdd.add(i);
                        }
                        Integer[] hits = myProject.getMetadataHybrid().search(queries, tsp.matchAll());

                        // remove excluded cols from list
                        // urmi
                        boolean[] excluded = excludedCopy;
                        if (excluded != null) {
                            List<Integer> temp = new ArrayList<>();
                            for (Integer i : hits) {
                                if (!excluded[i]) {
                                    temp.add(i);
                                }
                            }
                            hits = new Integer[temp.size()];
                            hits = temp.toArray(hits);
                        }
                        int index;
                        for (index = 0; index < hits.length; index++) {
                            result.add(hits[index]);
                            toAdd.remove(hits[index]);
                        }

                        return null;
                    }
                    catch(Exception e) {
                        return null;
                    }
                }
            }.start();

            // category
            if (result.size() < 1) {
                JOptionPane.showMessageDialog(null, "No hits found", "No hits", JOptionPane.INFORMATION_MESSAGE);
                return null;
            } else {
                List<String> hitsColumns = new ArrayList<>();
                // get datacolumn names
                for (int i : result) {
                    hitsColumns.add(myProject.getDataColumnHeader(i));
                }
                return hitsColumns;
            }

        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(null, "No hits found", "No hits", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        JButton source = (JButton) e.getSource();
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout(0, 0));

        JLabel label = new JLabel("Move Selected Items to..");
        //create a panel
        JPanel p =new JPanel();

        //String array to store weekdays
        String[] names = new String[limmaGroupPanels.size()];
        for (int i = 0; i < limmaGroupPanels.size(); i++) {
            names[i] = limmaGroupPanels.get(i).getName();
        }

        //create list
        JList groups = new JList(names);
        groups.setFixedCellHeight(25);
        groups.setFixedCellWidth(150);

        //set a selected index
        groups.setSelectedIndex(0);

        //add list to panel
        p.add(label, BorderLayout.NORTH);
        p.add(groups, BorderLayout.CENTER);

        JPanel btnPnl = new JPanel();
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                f.dispose();
            }
        });
        btnPnl.add(cancel);
        JButton ok = new JButton("Ok");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // buttons group id - 1 gives us the index of the corresponding group
                int callerIdx = Integer.parseInt(source.getName()) - 1;
                limmaGroupPanels.get(groups.getSelectedIndex()).addRows(limmaGroupPanels.get(callerIdx).getSelectedRows());
                limmaGroupPanels.get(callerIdx).removeSelectedRows();
                f.dispose();
            }
        });
        btnPnl.add(ok);

        f.add(p, BorderLayout.CENTER);
        f.add(btnPnl, BorderLayout.SOUTH);

        //set the size of frame
        int height = (names.length * 25) + 90;
        f.setSize(160,height);
        f.setLocation(getWidth() / 2, getHeight() / 2);
        f.setResizable(false);

        f.show();
    }

    private ArrayList<Integer> getIndices(List<String> listDC) {
        ArrayList<Integer> res = new ArrayList<>();
        String[] dataColumnheaders = myProject.getDataColumnHeaders();
        for (int i = 0; i < dataColumnheaders.length; i++) {
            if (listDC.contains(dataColumnheaders[i])) {
                res.add(i);
            }
        }
        return res;
    }
}
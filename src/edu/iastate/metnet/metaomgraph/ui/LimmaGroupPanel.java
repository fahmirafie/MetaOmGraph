package edu.iastate.metnet.metaomgraph.ui;

import edu.iastate.metnet.metaomgraph.*;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class LimmaGroupPanel extends JPanel  {

    public static int id;
    private int thisId;
    private JTable table;
    private JButton moveSelected;
    JLabel lblN;
    private Color SELECTIONBCKGRND = MetaOmGraph.getTableSelectionColor();
    private Color BCKGRNDCOLOR1 = MetaOmGraph.getTableColorEven();
    private Color BCKGRNDCOLOR2 = MetaOmGraph.getTableColorOdd();
    private Color HIGHLIGHTCOLOR = MetaOmGraph.getTableHighlightColor();
    private Color HYPERLINKCOLOR = MetaOmGraph.getTableHyperlinkColor();
    MetaOmProject myProject = MetaOmGraph.getActiveProject();
    MetadataHybrid mdob = myProject.getMetadataHybrid();

    public LimmaGroupPanel() {
        id++;
        thisId = id;
        setSize(200, 200);
        setLayout(new BorderLayout(0, 0));

        JLabel txtGroup = new JLabel();
        txtGroup.setText("Group" + id);
        JPanel topbtnPnl = new JPanel();

        JLabel lblGroupName = new JLabel("Group name:");
        topbtnPnl.add(lblGroupName);
        topbtnPnl.add(txtGroup);
        add(topbtnPnl, BorderLayout.NORTH);
        //topbtnPnl.setLayout(new BoxLayout(topbtnPnl, BoxLayout.LINE_AXIS));
        topbtnPnl.setLayout(new FlowLayout());
        lblN = new JLabel("n=0");
        lblN.setFont(new Font("Tahoma", Font.BOLD, 13));
        topbtnPnl.add(lblN);

        moveSelected = new JButton("Move Selected Rows");
        moveSelected.setName(""+thisId);
        moveSelected.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //moveSelectedRows();
            }
        });
        topbtnPnl.add(moveSelected);

        // add table2
        JScrollPane jscp = new JScrollPane();
        JTable tableGrp = initTableModel();
        // updateTableData(tableGrp, mdob.getMetadataCollection().getAllDataCols());
        updateTableData(null);
        jscp.setViewportView(tableGrp);
        add(jscp, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add");
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                java.util.List<String> queryRes = showSearchMetadataPanel();
                if (queryRes == null || queryRes.size() < 1) {
                    return;
                }
                // JOptionPane.showConfirmDialog(null, "match:" + queryRes.toString());
                addRows(queryRes);
            }
        });
        JPanel btnPnl = new JPanel(new FlowLayout());
        btnPnl.add(btnAdd);
        JButton btnRem = new JButton("Remove");
        btnRem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedRows();
            }
        });
        btnPnl.add(btnRem);
        JButton btnSearch = new JButton("Search");
        btnSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // search in list by metadatata
                java.util.List<String> queryRes = showSearchMetadataPanel();
                if (queryRes == null || queryRes.size() < 1) {
                    return;
                }
                // get intersection
                java.util.List<String> allRows = getAllRows();
                java.util.List<String> res = (List<String>) CollectionUtils.intersection(queryRes, allRows);
                // set selected and bring to top
                setSelectedRows(res);
            }
        });
        btnPnl.add(btnSearch);
        add(btnPnl, BorderLayout.SOUTH);
    }

    public int getId() {
        return thisId;
    }

    public static void resetId() {
        LimmaGroupPanel.id = 0;
    }

    public JButton getMoveSelectedBtn() {
        return moveSelected;
    }

    public String getName() {
        return "Group" + thisId;
    }

    private JTable initTableModel() {
        table = new JTable() {
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

    private void updateTableData(List<String> rows) {
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

    /**
     * get selected rows from a table
     *
     * @return
     */
    public List<String> getSelectedRows() {
        return getSelectedRows(false);
    }

    private List<String> getSelectedRows(boolean invert) {
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

    public void removeSelectedRows() {
        List<String> toKeep = getSelectedRows(true);
        // JOptionPane.showMessageDialog(null, "tokeep:" + toKeep.toString());
        updateTableData(toKeep);
        updateLabelN();

    }

    public void addRows(List<String> toAdd) {
        toAdd.addAll(getAllRows());
        // JOptionPane.showMessageDialog(null, "toAdd:" + toAdd.toString());
        updateTableData(toAdd);
        updateLabelN();
    }

    public List<String> getAllRows() {
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
        int n = getAllRows().size();
        lblN.setText("n=" + String.valueOf(n));
    }

    /**
     * @author urmi bring the matched items to top and set them as selected
     * @param res
     */
    private void setSelectedRows(List<String> res) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
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
            table.setRowSelectionInterval(0, total_matches - 1);
        }
    }

    private List<String> showSearchMetadataPanel() {
        // search datacolumns by metadata and add results
        // display query panel
        try {
            final TreeSearchQueryConstructionPanel tsp = new TreeSearchQueryConstructionPanel(myProject, false);
            final Metadata.MetadataQuery[] queries;
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
//                        boolean[] excluded = excludedCopy;
//                        if (excluded != null) {
//                            List<Integer> temp = new ArrayList<>();
//                            for (Integer i : hits) {
//                                if (!excluded[i]) {
//                                    temp.add(i);
//                                }
//                            }
//                            hits = new Integer[temp.size()];
//                            hits = temp.toArray(hits);
//                        }
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
}
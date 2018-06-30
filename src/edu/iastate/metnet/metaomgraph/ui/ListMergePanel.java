package edu.iastate.metnet.metaomgraph.ui;

import edu.iastate.metnet.metaomgraph.MetaOmProject;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;


public class ListMergePanel
        extends JPanel {
    private MetaOmProject myProject;
    private JCheckBox[] listBoxes;
    private JTextField nameField;

    public ListMergePanel(MetaOmProject project) {
        myProject = project;
        String[] lists = myProject.getGeneListNames();
        listBoxes = new JCheckBox[lists.length];
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, 1));
        for (int i = 0; i < listBoxes.length; i++) {
            listBoxes[i] = new JCheckBox(lists[i]);
            boxPanel.add(listBoxes[i]);
        }
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(new JLabel("New List Name: "), "West");
        nameField = new JTextField();

        namePanel.add(nameField, "Center");
        setLayout(new BorderLayout());
        add(new JScrollPane(boxPanel), "Center");
        add(namePanel, "South");
        setPreferredSize(new Dimension(500, 500));
    }

    public Integer[] getSelectedLists() {
        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < listBoxes.length; i++) {
            if (listBoxes[i].isSelected()) {
                result.add(Integer.valueOf(i));
            }
        }
        return result.toArray(new Integer[0]);
    }

    public Integer[] getMergedList() {
        TreeSet<Integer> result = new TreeSet();
        Integer[] selected = getSelectedLists();
        Integer[] arrayOfInteger1;
        int j = (arrayOfInteger1 = selected).length;
        for (int i = 0; i < j; i++) {
            int listNum = arrayOfInteger1[i].intValue();
            String listName = myProject.getGeneListNames()[listNum];
            int[] addUs = myProject.getGeneListRowNumbers(listName);
            for (int addMe : addUs) {
                result.add(Integer.valueOf(addMe));
            }
        }
        return result.toArray(new Integer[0]);
    }

    public String getMergedListName() {
        return nameField.getText();
    }

    public static TreeMap<String, Integer[]> showMergeDialog(MetaOmProject project) {
        final ListMergePanel mergePanel = new ListMergePanel(project);
        JPanel buttonPanel = new JPanel();


        final JDialog dialog = new JDialog((Frame) null, "Merge Lists", true);
        final TreeMap<String, Integer[]> result = new TreeMap();
        final JLabel statusMessage = new JLabel("<html>Select two or more lists to merge</html>");
        JButton okButton = new JButton(new AbstractAction("OK") {
            public void actionPerformed(ActionEvent e) {
                Integer[] selected = mergePanel.getSelectedLists();
                if (selected.length <= 1) {
                    StringBuilder text = new StringBuilder(selected.length + " list");
                    if (selected.length == 0) {
                        text.append("s");
                    }
                    text.append(" selected.  Must select at least 2");
                    statusMessage.setText("<html><font color=#FF0000>" + text.toString() + "</font></html>");
                    return;
                }
                String listName = mergePanel.getMergedListName().trim();
                if ("".equals(listName)) {
                    statusMessage.setText("<html><font color=#FF0000>Must enter a new list name</font></html>");
                    return;
                }
                Integer[] merged = mergePanel.getMergedList();
                result.put(listName, merged);
                int[] intMerged = new int[merged.length];
                for (int i = 0; i < intMerged.length; i++) {
                    intMerged[i] = merged[i].intValue();
                }
                if (mergePanel.myProject.addGeneList(listName, intMerged, true)) {
                    dialog.dispose();
                }

            }
        });
        JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }

        });
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusMessage, "North");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        statusPanel.add(buttonPanel, "Center");
        dialog.add(mergePanel, "Center");
        dialog.add(statusPanel, "South");
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return result;
    }
}

package org.codehaus.mojo.license.extended.spreadsheet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableCellRange;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import org.odftoolkit.odfdom.dom.element.table.TableTableColumnElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableColumnGroupElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.w3c.dom.Node;

/**
 * Utility class for cell merging and group building for {@link CalcFileWriter}.
 */
class CalcColumnGroupBuilder {
    private CalcColumnGroupBuilder() {}

    /**
     * Custom attribute name to store pending column groups, before they are added to the table.
     */
    private static final String PENDING_COLUMN_GROUPS_KEY = CalcFileWriter.class.getName() + ".pendingColumnGroups";

    /**
     * Adds a column group to the table, which will be applied when {@link #applyPendingColumnGroups(OdfTable)}
     * is called.
     *
     * @param table Table to add the column group to.
     * @param startColumn Inclusive start column index.
     * @param endColumn Exclusive end column index, i.e. the first column index that is not part of this group.
     */
    private static void addColumnGroup(OdfTable table, int startColumn, int endColumn) {
        if (endColumn - startColumn < 2) {
            return;
        }
        getPendingColumnGroups(table).add(new ColumnGroup(startColumn, endColumn));
    }

    @SuppressWarnings("unchecked")
    private static List<ColumnGroup> getPendingColumnGroups(OdfTable table) {
        TableTableElement tableElement = table.getOdfElement();
        List<ColumnGroup> groups = (List<ColumnGroup>) tableElement.getUserData(PENDING_COLUMN_GROUPS_KEY);
        if (groups == null) {
            groups = new ArrayList<>();
            tableElement.setUserData(PENDING_COLUMN_GROUPS_KEY, groups, null);
        }
        return groups;
    }

    static void applyPendingColumnGroups(OdfTable table) {
        TableTableElement tableElement = table.getOdfElement();
        List<ColumnGroup> groups = getPendingColumnGroups(table);
        if (groups.isEmpty()) {
            return;
        }

        // Make sure all columns exist before rebuilding the table's column structure.
        int columnCount = table.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            // Auto-Extend the number of columns, so no columns are missing.
            table.getColumnByIndex(i);
        }

        // Collect the current direct column nodes so they can be reinserted in grouped order.
        List<TableTableColumnElement> columnElements = getDirectColumnElements(tableElement);
        if (columnElements.isEmpty()) {
            tableElement.setUserData(PENDING_COLUMN_GROUPS_KEY, null, null);
            return;
        }

        // Build the nested group tree and replace the flat column sequence with grouped nodes.
        ColumnGroup rootGroup = buildColumnGroupTree(columnElements.size(), groups);
        Node firstNonColumnNode = tableElement.getFirstChild();
        while (firstNonColumnNode instanceof TableTableColumnElement) {
            firstNonColumnNode = firstNonColumnNode.getNextSibling();
        }

        // Remove the existing flat column nodes before inserting the grouped structure.
        for (TableTableColumnElement columnElement : columnElements) {
            tableElement.removeChild(columnElement);
        }

        Node rootFragment = tableElement.getOwnerDocument().createDocumentFragment();
        appendColumns(rootFragment, tableElement, columnElements, rootGroup);
        if (firstNonColumnNode == null) {
            tableElement.appendChild(rootFragment);
        } else {
            tableElement.insertBefore(rootFragment, firstNonColumnNode);
        }
        // There is no explicit "remove" method for the userData, just setting it to null does that.
        tableElement.setUserData(PENDING_COLUMN_GROUPS_KEY, null, null);
    }

    private static @NonNull List<TableTableColumnElement> getDirectColumnElements(
            @NonNull TableTableElement tableElement) {
        List<TableTableColumnElement> columnElements = new ArrayList<>();
        for (Node child = tableElement.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof TableTableColumnElement) {
                columnElements.add((TableTableColumnElement) child);
            }
        }
        return columnElements;
    }

    private static @NonNull ColumnGroup buildColumnGroupTree(int columnCount, @NonNull List<ColumnGroup> groups) {
        List<ColumnGroup> sortedGroups = new ArrayList<>(groups);
        // Sort by start column, and for equal starts put outer groups before inner groups.
        sortedGroups.sort(java.util.Comparator.comparingInt((ColumnGroup group) -> group.startColumn)
                .thenComparing(java.util.Comparator
                        /* Put the enclosing parent groups before the nested child groups,
                        so the parent group is always before the child group. */
                        .comparingInt((ColumnGroup group) -> group.endColumn)
                        .reversed()));

        // Virtual root group, around _all_ columns.
        ColumnGroup rootGroup = new ColumnGroup(0, columnCount);

        Deque<ColumnGroup> stack = new ArrayDeque<>();
        stack.addFirst(rootGroup);
        for (ColumnGroup group : sortedGroups) {
            // 1st run: parent = root. Following runs: parent = Last subgroup.
            ColumnGroup parentGroup = stack.peekFirst();
            // 1st run: 1st subgroup start < always root/parent.end.
            // Following run: subgroup start < previous subgroup.end, if it's a parent group. Otherwise, enter the loop.
            while (parentGroup != null && group.startColumn >= parentGroup.endColumn) {
                /* This group is outside the current parent range, so move up the nesting stack
                until we find the correct enclosing parent. */
                stack.removeFirst();
                parentGroup = stack.peekFirst();
            }
            if (parentGroup == null
                    || group.startColumn < parentGroup.startColumn
                    || group.endColumn > parentGroup.endColumn) {
                throw new IllegalArgumentException("Column groups must be properly nested without crossings.");
            }
            // Attach the group to its enclosing parent in the tree.
            parentGroup.nestedGroups.add(group);
            // Track the current group so the next nested group can attach beneath it.
            stack.addFirst(group);
        }
        return rootGroup;
    }

    private static void appendColumns(
            Node parentNode,
            TableTableElement tableElement,
            List<TableTableColumnElement> columnElements,
            @NonNull ColumnGroup group) {
        // Emit any plain columns that appear before each nested group.
        int columnIndex = group.startColumn;
        for (ColumnGroup nestedGroup : group.nestedGroups) {
            while (columnIndex < nestedGroup.startColumn) {
                parentNode.appendChild(columnElements.get(columnIndex++));
            }
            // Wrap the nested range in a column-group node, then recurse into it.
            TableTableColumnGroupElement groupElement = tableElement.newTableTableColumnGroupElement();
            appendColumns(groupElement, tableElement, columnElements, nestedGroup);
            parentNode.appendChild(groupElement);
            columnIndex = nestedGroup.endColumn;
        }
        // Emit any trailing columns that follow the last nested group.
        while (columnIndex < group.endColumn) {
            parentNode.appendChild(columnElements.get(columnIndex++));
        }
    }

    static void createMergedCellsInRow(
            OdfTable table,
            int startColumn,
            int endColumn,
            OdfTableRow row,
            String cellValue,
            int rowIndex,
            String styleName) {
        OdfTableCell cell = createCellsInRow(startColumn, endColumn, row);
        if (cell == null) {
            return;
        }
        final boolean merge = endColumn - 1 > startColumn;

        if (merge) {
            // Merge the covered cells.
            OdfTableCellRange cellRange = table.getCellRangeByPosition(startColumn, rowIndex, endColumn - 1, rowIndex);
            cellRange.merge();
            /* Add a column group, so they can be easily hidden in Calc.
            The column group will only actually be inserted later, in applyPendingColumnGroups(...). */
            addColumnGroup(table, startColumn, endColumn);
        }

        // Set value and style only after merge
        cell.setStringValue(cellValue);
        cell.getOdfElement().setStyleName(styleName);
    }

    private static OdfTableCell createCellsInRow(int startColumn, int exclusiveEndColumn, OdfTableRow inRow) {
        OdfTableCell firstCell = null;
        for (int i = startColumn; i < exclusiveEndColumn; i++) {
            OdfTableCell cell = inRow.getCellByIndex(i);
            if (i == startColumn) {
                firstCell = cell;
            }
        }
        return firstCell;
    }

    private static final class ColumnGroup {
        /**
         * Inclusive start column index.
         */
        private final int startColumn;
        /**
         * Exclusive end column index, i.e. the first column index that is not part of this group.
         */
        private final int endColumn;

        private final List<ColumnGroup> nestedGroups = new ArrayList<>();

        /**
         * Creates a new column group.
         *
         * @param startColumn Inclusive start column index.
         * @param endColumn   Exclusive end column index, i.e. the first column index that is not part of this group.
         */
        private ColumnGroup(int startColumn, int endColumn) {
            this.startColumn = startColumn;
            this.endColumn = endColumn;
        }

        @Override
        public String toString() {
            return startColumn + ".." + endColumn;
        }
    }
}

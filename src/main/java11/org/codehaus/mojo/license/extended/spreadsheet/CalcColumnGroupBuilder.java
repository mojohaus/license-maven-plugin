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
     * Custom attribute name to store pending column groups, before they are applied to the table.
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

    /**
     * Returns the list of pending column groups for the given table, creating it if absent.
     *
     * @param table the table whose pending column groups should be retrieved.
     * @return the mutable list of pending {@link ColumnGroup}s attached to the table.
     */
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

    /**
     * Applies all pending column groups to the table, restructuring the flat column sequence into a nested
     * {@code table:table-column-group} hierarchy.
     * <p>
     * Column groups previously registered via {@link #addColumnGroup(OdfTable, int, int)} are collected, sorted, and
     * inserted into the table's XML structure. This method must be called after all rows and {@link ColumnGroup}s have
     * been added to the table.
     *
     * @param table the table to apply the pending column groups to.
     */
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

        /* Build a document fragment that contains the regrouped column structure.
        This lets us replace the flat sequence of <table:table-column> nodes
        with the nested <table:table-column-group> hierarchy in one atomic insert. */
        Node rootFragment = tableElement.getOwnerDocument().createDocumentFragment();
        appendColumns(rootFragment, tableElement, columnElements, rootGroup);

        if (firstNonColumnNode == null) {
            /* No non-column nodes were found, so this is the end of the table definition.
            Append the rebuilt column structure at the end of the table element. */
            tableElement.appendChild(rootFragment);
        } else {
            /* Insert the regrouped columns before the first non-column node, preserving
            the ordering of any following table content/metadata. */
            tableElement.insertBefore(rootFragment, firstNonColumnNode);
        }
        // There is no explicit "remove" method for the userData, just setting it to null does that.
        tableElement.setUserData(PENDING_COLUMN_GROUPS_KEY, null, null);
    }

    /**
     * Returns the direct table-column elements in document order.
     * <p>
     * This inspects only the immediate children of the table definition and ignores
     * any nested column-group nodes or other table content so the flat column list
     * can be regrouped consistently.
     *
     * @param tableElement the table definition whose direct column elements should be read.
     * @return the direct {@code table:table-column} elements in the order they appear
     * in the document.
     */
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

    /**
     * Builds a nested tree of {@link ColumnGroup}s from a flat list of groups.
     * <p>
     * The groups are sorted by start column and then by end (outermost first),
     * and are attached to their enclosing parent via a virtual root group that
     * spans all columns.
     *
     * @param columnCount total number of columns in the table.
     * @param groups      flat list of column groups to nest; must be properly nested without crossings.
     * @return the virtual root {@link ColumnGroup} whose {@code nestedGroups} contain the full tree.
     * @throws IllegalArgumentException if two groups cross without one fully containing the other.
     */
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

    /**
     * Appends the column structure for the given group to the target node.
     * <p>
     * Plain columns are emitted in document order, and nested groups are wrapped
     * in {@code table:table-column-group} elements recursively.
     *
     * @param parentNode the node that will receive the appended column structure.
     * @param tableElement the table used to create new column-group elements.
     * @param columnElements the flat list of column elements in document order.
     * @param group the column group to append.
     */
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

    /**
     * Creates the cells for the given column range in the target row and merges them when the range spans
     * more than one column.
     * <p>
     * When a merge happens, the corresponding pending column group is also registered so Calc can expose
     * that range as a hideable column group once
     * {@link #applyPendingColumnGroups(OdfTable)} runs.
     *
     * @param table the table containing the row and merged cell range.
     * @param startColumn inclusive start column index.
     * @param endColumn exclusive end column index.
     * @param row the row in which the cells should be merged.
     * @param cellValue the string value to assign to the leading cell.
     * @param rowIndex the row index used to address the merge range in the table.
     * @param styleName the style name to apply to the leading cell after merging.
     */
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

    /**
     * Stores nested column group ranges, to be later applied to the table.
     */
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

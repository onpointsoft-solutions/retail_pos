package com.mobilemeals.pos;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model backing the order-entry line-item grid.
 */
public class OrderTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
            "Code", "Name", "Qty", "Unit Price", "Total"
    };

    private final List<OrderEntryPanelSimple.OrderItem> items = new ArrayList<>();

    // -------------------------------------------------------------------------
    // AbstractTableModel
    // -------------------------------------------------------------------------

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        OrderEntryPanelSimple.OrderItem item = items.get(rowIndex);
        switch (columnIndex) {
            case 0: return item.getCode();
            case 1: return item.getName();
            case 2: return item.getQuantity();
            case 3: return String.format("KES %.2f", item.getUnitPrice());
            case 4: return String.format("KES %.2f", item.getQuantity() * item.getUnitPrice());
            default: return "";
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Allow editing quantity column only
        return columnIndex == 2;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 2) {
            try {
                int qty = Integer.parseInt(aValue.toString());
                if (qty > 0) {
                    items.get(rowIndex).setQuantity(qty);
                    fireTableCellUpdated(rowIndex, columnIndex);
                    fireTableCellUpdated(rowIndex, 4); // refresh total column
                }
            } catch (NumberFormatException ignored) {
                // reject invalid input silently
            }
        }
    }

    // -------------------------------------------------------------------------
    // Mutation helpers
    // -------------------------------------------------------------------------

    public void addItem(OrderEntryPanelSimple.OrderItem item) {
        // If the item already exists, increment its quantity instead
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getCode().equals(item.getCode())) {
                items.get(i).setQuantity(items.get(i).getQuantity() + item.getQuantity());
                fireTableRowsUpdated(i, i);
                return;
            }
        }
        items.add(item);
        fireTableRowsInserted(items.size() - 1, items.size() - 1);
    }

    public void removeItem(OrderEntryPanelSimple.OrderItem item) {
        int index = items.indexOf(item);
        if (index >= 0) {
            items.remove(index);
            fireTableRowsDeleted(index, index);
        }
    }

    public void removeRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < items.size()) {
            items.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    public void clearItems() {
        items.clear();
        fireTableDataChanged();
    }

    public List<OrderEntryPanelSimple.OrderItem> getItems() {
        return new ArrayList<>(items);
    }

    public double getGrandTotal() {
        return items.stream()
                .mapToDouble(i -> i.getQuantity() * i.getUnitPrice())
                .sum();
    }
}
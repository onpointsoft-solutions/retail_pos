<?php
declare(strict_types=1);

class SaleController {
    public function index(array $params, array $body): array {
        $page = (int)($params['page'] ?? 1);
        $limit = (int)($params['limit'] ?? DEFAULT_PAGE_SIZE);
        $offset = ($page - 1) * $limit;
        
        $startDate = $params['start_date'] ?? date('Y-m-d');
        $endDate = $params['end_date'] ?? date('Y-m-d');
        
        $sales = Database::fetchAll(
            'SELECT * FROM sales WHERE DATE(created_at) BETWEEN ? AND ? ORDER BY created_at DESC LIMIT ? OFFSET ?',
            [$startDate, $endDate, $limit, $offset]
        );
        
        $total = Database::fetchOne(
            'SELECT COUNT(*) as count FROM sales WHERE DATE(created_at) BETWEEN ? AND ?',
            [$startDate, $endDate]
        )['count'] ?? 0;
        
        return [
            'data' => $sales,
            'meta' => [
                'total' => (int)$total,
                'page' => $page,
                'limit' => $limit,
                'pages' => ceil($total / $limit),
            ],
        ];
    }

    public function store(array $params, array $body): array {
        $items = $body['items'] ?? [];
        $customerId = $body['customer_id'] ?? null;
        $paymentMethod = $body['payment_method'] ?? 'CASH';
        $cashTendered = (float)($body['cash_tendered'] ?? 0);
        $discountAmount = (float)($body['discount_amount'] ?? 0);
        
        if (empty($items)) {
            http_response_code(400);
            return ['error' => 'Sale items are required'];
        }
        
        $user = Auth::user();
        if (!$user) {
            http_response_code(401);
            return ['error' => 'Unauthorized'];
        }
        
        Database::beginTransaction();
        
        try {
            $saleId = bin2hex(random_bytes(16));
            $receiptNumber = 'REC-' . date('Ymd-His') . '-' . strtoupper(substr($saleId, 0, 4));
            
            $subtotal = 0;
            $taxAmount = 0;
            
            foreach ($items as $item) {
                $lineTotal = ($item['unit_price'] * $item['quantity']) - ($item['discount'] ?? 0);
                $subtotal += $lineTotal;
                $taxAmount += $lineTotal * (($item['tax_rate'] ?? 0) / 100);
            }
            
            $grandTotal = $subtotal - $discountAmount + $taxAmount;
            $changeAmount = max(0, $cashTendered - $grandTotal);
            
            // Insert sale
            Database::execute(
                'INSERT INTO sales (id, receipt_number, cashier_id, cashier_name, customer_id, subtotal, 
                 discount_amount, tax_amount, grand_total, payment_method, cash_tendered, change_amount, status, created_at, updated_at) 
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
                [
                    $saleId,
                    $receiptNumber,
                    $user->getId(),
                    $user->getFullName() ?? $user->getUsername(),
                    $customerId,
                    $subtotal,
                    $discountAmount,
                    $taxAmount,
                    $grandTotal,
                    $paymentMethod,
                    $cashTendered,
                    $changeAmount,
                    'COMPLETED',
                    date('Y-m-d H:i:s'),
                    date('Y-m-d H:i:s'),
                ]
            );
            
            // Insert sale items and update stock
            foreach ($items as $item) {
                $itemId = bin2hex(random_bytes(16));
                $lineTotal = ($item['unit_price'] * $item['quantity']) - ($item['discount'] ?? 0);
                
                Database::execute(
                    'INSERT INTO sale_items (id, sale_id, product_id, product_name, product_sku, quantity, 
                     unit_price, buying_price, discount, tax_rate, line_total) 
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
                    [
                        $itemId,
                        $saleId,
                        $item['product_id'] ?? null,
                        $item['product_name'],
                        $item['product_sku'] ?? null,
                        $item['quantity'],
                        $item['unit_price'],
                        $item['buying_price'] ?? 0,
                        $item['discount'] ?? 0,
                        $item['tax_rate'] ?? 0,
                        $lineTotal,
                    ]
                );
                
                // Update product stock
                if (!empty($item['product_id'])) {
                    Database::execute(
                        'UPDATE products SET current_stock = current_stock - ? WHERE id = ?',
                        [$item['quantity'], $item['product_id']]
                    );
                    
                    // Record inventory movement
                    $movementId = bin2hex(random_bytes(16));
                    Database::execute(
                        'INSERT INTO inventory_movements (id, product_id, product_name, type, quantity, reason, user_id, created_at, updated_at) 
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
                        [
                            $movementId,
                            $item['product_id'],
                            $item['product_name'],
                            'SALE',
                            -$item['quantity'],
                            'Sale: ' . $receiptNumber,
                            $user->getId(),
                            date('Y-m-d H:i:s'),
                            date('Y-m-d H:i:s'),
                        ]
                    );
                }
            }
            
            Database::commit();
            
            http_response_code(201);
            return [
                'id' => $saleId,
                'receipt_number' => $receiptNumber,
                'subtotal' => $subtotal,
                'tax_amount' => $taxAmount,
                'grand_total' => $grandTotal,
                'change_amount' => $changeAmount,
            ];
            
        } catch (Exception $e) {
            Database::rollback();
            throw $e;
        }
    }

    public function show(array $params, array $body): array {
        $sale = Database::fetchOne(
            'SELECT * FROM sales WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$sale) {
            http_response_code(404);
            return ['error' => 'Sale not found'];
        }
        
        $items = Database::fetchAll(
            'SELECT * FROM sale_items WHERE sale_id = ?',
            [$params['id']]
        );
        
        return [
            'sale' => $sale,
            'items' => $items,
        ];
    }
}

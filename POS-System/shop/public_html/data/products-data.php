<?php

require_once '../includes/config.php';

/**
 * Fetch all products from database
 * @return array
 */
function fetchProductsFromDatabase() {
    $pdo = getDbConnection();
    
    $sql = "SELECT 
                p.*,
                c.name as category_name,
                c.id as category_id,
                s.name as supplier_name
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            WHERE p.deleted_at IS NULL
            ORDER BY p.created_at DESC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $dbProducts = $stmt->fetchAll();
    
    $products = [];
    foreach ($dbProducts as $product) {
        // Generate slug from SKU or name
        $slug = !empty($product['sku']) ? strtolower(preg_replace('/[^a-zA-Z0-9-]/', '-', $product['sku'])) 
                : strtolower(preg_replace('/[^a-zA-Z0-9-]/', '-', $product['name']));
        
        // Generate category slug from category name
        $categorySlug = !empty($product['category_name']) ? strtolower(preg_replace('/[^a-zA-Z0-9-]/', '-', $product['category_name'])) : 'uncategorized';
        
        $products[] = [
            'slug' => $slug,
            'category' => $categorySlug,
            'name' => $product['name'],
            'brand' => $product['supplier_name'] ?? 'Generic',
            'price' => (float) $product['selling_price'],
            'compare_price' => !empty($product['wholesale_price']) && $product['wholesale_price'] > $product['selling_price'] 
                ? (float) $product['wholesale_price'] 
                : null,
            'unit' => $product['unit'] ?? 'pcs',
            'stock' => (int) $product['current_stock'],
            'description' => $product['description'] ?? '',
            'image' => !empty($product['image_path']) ? '/' . $product['image_path'] : '/assets/product-images/SampleProduct.png',
            'featured' => $product['status'] === 'active',
            'rating' => 4.5, // Default rating since not in database
            'review_count' => 0, // Default since not in database
            'sku' => $product['sku'],
            'barcode' => $product['barcode'],
            'buying_price' => (float) $product['buying_price'],
            'tax_rate' => (float) $product['tax_rate'],
            'discount' => (float) $product['discount']
        ];
    }
    
    return $products;
}

// Initialize products from database
$shop_products = fetchProductsFromDatabase();

/**
 * Get all products or filter by category
 * @param string|null $categorySlug
 * @return array
 */
function shop_get_products($categorySlug = null) {
    global $shop_products;
    
    if ($categorySlug === null) {
        return $shop_products;
    }
    
    return array_filter($shop_products, function($product) use ($categorySlug) {
        return $product['category'] === $categorySlug;
    });
}

/**
 * Get a single product by slug
 * @param string $slug
 * @return array|null
 */
function shop_get_product_by_slug($slug) {
    global $shop_products;
    
    foreach ($shop_products as $product) {
        if ($product['slug'] === $slug) {
            return $product;
        }
    }
    
    return null;
}

/**
 * Get featured products
 * @param int $count
 * @return array
 */
function shop_get_featured_products($count = 8) {
    global $shop_products;
    
    $featured = array_filter($shop_products, function($product) {
        return $product['featured'] === true;
    });
    
    return array_slice($featured, 0, $count);
}

/**
 * Get related products from the same category
 * @param string $slug
 * @param int $count
 * @return array
 */
function shop_get_related_products($slug, $count = 4) {
    global $shop_products;
    
    $product = shop_get_product_by_slug($slug);
    if (!$product) {
        return [];
    }
    
    $related = array_filter($shop_products, function($p) use ($product, $slug) {
        return $p['category'] === $product['category'] && $p['slug'] !== $slug;
    });
    
    return array_slice($related, 0, $count);
}

/**
 * Search products by name or brand
 * @param string $query
 * @return array
 */
function shop_search_products($query) {
    global $shop_products;
    
    $query = strtolower(trim($query));
    
    if (empty($query)) {
        return [];
    }
    
    return array_filter($shop_products, function($product) use ($query) {
        $name = strtolower($product['name']);
        $brand = strtolower($product['brand']);
        
        return strpos($name, $query) !== false || strpos($brand, $query) !== false;
    });
}

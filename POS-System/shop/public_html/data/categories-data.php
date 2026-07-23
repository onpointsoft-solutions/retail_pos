<?php

require_once '../includes/config.php';

/**
 * Fetch all categories from database
 * @return array
 */
function fetchCategoriesFromDatabase() {
    $pdo = getDbConnection();
    
    $sql = "SELECT * FROM categories WHERE deleted_at IS NULL ORDER BY name ASC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $dbCategories = $stmt->fetchAll();
    
    $categories = [];
    $iconMap = [
        'household' => '🏠',
        'general' => '📦',
        'food' => '🍔',
        'food & beverages' => '🍔',
        'clothing' => '👕',
        'electronics' => '📱',
        'default' => '📦'
    ];
    
    foreach ($dbCategories as $category) {
        // Generate slug from category name
        $slug = strtolower(preg_replace('/[^a-zA-Z0-9-]/', '-', $category['name']));
        
        // Map icon based on category name
        $icon = '📦'; // default
        $categoryNameLower = strtolower($category['name']);
        foreach ($iconMap as $key => $value) {
            if (strpos($categoryNameLower, $key) !== false) {
                $icon = $value;
                break;
            }
        }
        
        $categories[] = [
            'slug' => $slug,
            'name' => $category['name'],
            'icon' => $icon,
            'image' => '/assets/images/categories/' . $slug . '.jpg',
            'id' => $category['id'],
            'description' => $category['description']
        ];
    }
    
    return $categories;
}

// Initialize categories from database
$shop_categories = fetchCategoriesFromDatabase();

/**
 * Get all categories
 * @return array
 */
function shop_get_categories() {
    global $shop_categories;
    return $shop_categories;
}

/**
 * Get a single category by slug
 * @param string $slug
 * @return array|null
 */
function shop_get_category_by_slug($slug) {
    global $shop_categories;
    foreach ($shop_categories as $category) {
        if ($category['slug'] === $slug) {
            return $category;
        }
    }
    return null;
}

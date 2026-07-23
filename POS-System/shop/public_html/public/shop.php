<?php
require_once '../data/categories-data.php';
require_once '../data/products-data.php';

// Read query params
$selectedCategory = isset($_GET['category']) ? $_GET['category'] : null;
$searchQuery = isset($_GET['search']) ? trim($_GET['search']) : '';
$sortBy = isset($_GET['sort']) ? $_GET['sort'] : 'default';
$currentPage = isset($_GET['page']) ? max(1, intval($_GET['page'])) : 1;
$itemsPerPage = 12;

// Get base product list
if (!empty($searchQuery)) {
    $products = shop_search_products($searchQuery);
    
    // Further filter by category if both search and category are set
    if ($selectedCategory) {
        $products = array_filter($products, function($product) use ($selectedCategory) {
            return $product['category'] === $selectedCategory;
        });
        // Re-index array after filtering
        $products = array_values($products);
    }
} elseif ($selectedCategory) {
    $products = shop_get_products($selectedCategory);
    // Re-index array
    $products = array_values($products);
} else {
    $products = shop_get_products();
}

// Apply sorting
switch ($sortBy) {
    case 'price-low':
        usort($products, function($a, $b) {
            return $a['price'] - $b['price'];
        });
        break;
    case 'price-high':
        usort($products, function($a, $b) {
            return $b['price'] - $a['price'];
        });
        break;
    case 'name':
        usort($products, function($a, $b) {
            return strcasecmp($a['name'], $b['name']);
        });
        break;
    case 'rating':
        usort($products, function($a, $b) {
            return $b['rating'] - $a['rating'];
        });
        break;
    default:
        // Keep original order
        break;
}

// Pagination calculation
$totalProducts = count($products);
$totalPages = ceil($totalProducts / $itemsPerPage);
$currentPage = min($currentPage, max(1, $totalPages)); // Ensure valid page
$offset = ($currentPage - 1) * $itemsPerPage;
$displayProducts = array_slice($products, $offset, $itemsPerPage);

// Get category name for display
$categoryName = null;
if ($selectedCategory) {
    $categoryData = shop_get_category_by_slug($selectedCategory);
    if ($categoryData) {
        $categoryName = $categoryData['name'];
    }
}

// Build page title
if ($categoryName) {
    $pageTitle = $categoryName . " - Victorious General Shop";
} elseif (!empty($searchQuery)) {
    $pageTitle = "Search: " . htmlspecialchars($searchQuery) . " - Victorious General Shop";
} else {
    $pageTitle = "Shop All Products - Victorious General Shop";
}

include '../includes/header.php';

// Get all categories for sidebar
$categories = shop_get_categories();

// Helper function to build URL with query params
function buildUrl($params = []) {
    global $selectedCategory, $searchQuery, $sortBy, $currentPage;
    
    $defaults = [
        'category' => $selectedCategory,
        'search' => $searchQuery,
        'sort' => $sortBy,
        'page' => 1
    ];
    
    $merged = array_merge($defaults, $params);
    
    // Remove empty values
    $merged = array_filter($merged, function($value) {
        return $value !== null && $value !== '';
    });
    
    // Remove page if it's 1
    if (isset($merged['page']) && $merged['page'] == 1) {
        unset($merged['page']);
    }
    
    // Remove sort if it's default
    if (isset($merged['sort']) && $merged['sort'] === 'default') {
        unset($merged['sort']);
    }
    
    return 'shop.php' . (count($merged) > 0 ? '?' . http_build_query($merged) : '');
}
?>

<!-- Main Content with Alpine.js for mobile filter toggle -->
<div x-data="{ filtersOpen: false }">

<!-- 1. PAGE HEADER BAND -->
<section class="bg-surface-gray py-8">
    <div class="container mx-auto px-4">
        <!-- Breadcrumb -->
        <nav class="mb-4">
            <ol class="flex items-center space-x-2 text-sm font-body text-gray-500">
                <li><a href="index.php" class="hover:text-primary-gold">Home</a></li>
                <li>&gt;</li>
                <li><a href="shop.php" class="hover:text-primary-gold">Shop</a></li>
                <?php if ($categoryName): ?>
                    <li>&gt;</li>
                    <li class="text-charcoal font-medium"><?php echo htmlspecialchars($categoryName); ?></li>
                <?php endif; ?>
            </ol>
        </nav>
        
        <!-- Heading -->
        <h1 class="font-heading font-bold text-charcoal text-3xl md:text-4xl mb-2">
            <?php 
            if ($categoryName) {
                echo htmlspecialchars($categoryName);
            } elseif (!empty($searchQuery)) {
                echo 'Search Results: "' . htmlspecialchars($searchQuery) . '"';
            } else {
                echo 'Shop All Products';
            }
            ?>
        </h1>
        
        <!-- Result Count -->
        <p class="font-body text-gray-500 text-sm">
            Showing <?php echo number_format($totalProducts); ?> product<?php echo $totalProducts !== 1 ? 's' : ''; ?>
        </p>
    </div>
</section>

<!-- 2. MAIN LAYOUT -->
<section class="bg-surface-gray py-8">
    <div class="container mx-auto px-4">
        
        <!-- Mobile Filters Button -->
        <div class="lg:hidden mb-6">
            <button @click="filtersOpen = !filtersOpen" 
                    class="w-full bg-neutral-white border border-gray-300 text-charcoal px-4 py-3 rounded-lg font-heading font-semibold flex items-center justify-center space-x-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"/>
                </svg>
                <span>Filters & Sort</span>
            </button>
        </div>
        
        <div class="flex flex-col lg:flex-row gap-8">
            
            <!-- LEFT SIDEBAR -->
            <aside class="lg:w-64 flex-shrink-0" 
                   :class="{ 'hidden': !filtersOpen }" 
                   x-show="filtersOpen || window.innerWidth >= 1024"
                   x-transition>
                <div class="bg-neutral-white rounded-2xl shadow-sm p-6 lg:sticky lg:top-24">
                    
                    <!-- Search -->
                    <div class="mb-6">
                        <h3 class="font-heading font-semibold text-charcoal text-base mb-3">Search</h3>
                        <form method="get" action="shop.php">
                            <?php if ($selectedCategory): ?>
                                <input type="hidden" name="category" value="<?php echo htmlspecialchars($selectedCategory); ?>">
                            <?php endif; ?>
                            <?php if ($sortBy !== 'default'): ?>
                                <input type="hidden" name="sort" value="<?php echo htmlspecialchars($sortBy); ?>">
                            <?php endif; ?>
                            <input type="text" 
                                   name="search" 
                                   value="<?php echo htmlspecialchars($searchQuery); ?>"
                                   placeholder="Search products..." 
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-gold text-sm">
                        </form>
                    </div>
                    
                    <!-- Categories -->
                    <div class="mb-6">
                        <h3 class="font-heading font-semibold text-charcoal text-base mb-3">Categories</h3>
                        <ul class="space-y-2">
                            <li>
                                <a href="<?php echo buildUrl(['category' => null]); ?>" 
                                   class="flex items-center font-body text-sm py-2 px-3 rounded-lg transition-colors <?php echo !$selectedCategory ? 'text-primary-gold bg-gold-light bg-opacity-10 border-l-2 border-primary-gold' : 'text-charcoal hover:text-primary-gold hover:bg-gray-50'; ?>">
                                    All Products
                                </a>
                            </li>
                            <?php foreach ($categories as $category): ?>
                                <li>
                                    <a href="<?php echo buildUrl(['category' => $category['slug']]); ?>" 
                                       class="flex items-center font-body text-sm py-2 px-3 rounded-lg transition-colors <?php echo $selectedCategory === $category['slug'] ? 'text-primary-gold bg-gold-light bg-opacity-10 border-l-2 border-primary-gold' : 'text-charcoal hover:text-primary-gold hover:bg-gray-50'; ?>">
                                        <span class="mr-2"><?php echo $category['icon']; ?></span>
                                        <?php echo htmlspecialchars($category['name']); ?>
                                    </a>
                                </li>
                            <?php endforeach; ?>
                        </ul>
                    </div>
                    
                    <!-- Sort By -->
                    <div class="mb-6">
                        <h3 class="font-heading font-semibold text-charcoal text-base mb-3">Sort By</h3>
                        <form method="get" action="shop.php" id="sortForm">
                            <?php if ($selectedCategory): ?>
                                <input type="hidden" name="category" value="<?php echo htmlspecialchars($selectedCategory); ?>">
                            <?php endif; ?>
                            <?php if (!empty($searchQuery)): ?>
                                <input type="hidden" name="search" value="<?php echo htmlspecialchars($searchQuery); ?>">
                            <?php endif; ?>
                            <select name="sort" 
                                    onchange="this.form.submit()"
                                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-gold text-sm">
                                <option value="default" <?php echo $sortBy === 'default' ? 'selected' : ''; ?>>Default</option>
                                <option value="price-low" <?php echo $sortBy === 'price-low' ? 'selected' : ''; ?>>Price: Low to High</option>
                                <option value="price-high" <?php echo $sortBy === 'price-high' ? 'selected' : ''; ?>>Price: High to Low</option>
                                <option value="name" <?php echo $sortBy === 'name' ? 'selected' : ''; ?>>Name: A-Z</option>
                                <option value="rating" <?php echo $sortBy === 'rating' ? 'selected' : ''; ?>>Highest Rated</option>
                            </select>
                        </form>
                    </div>
                    
                    <!-- Clear All Filters -->
                    <?php if ($selectedCategory || !empty($searchQuery) || $sortBy !== 'default'): ?>
                        <div>
                            <a href="shop.php" class="font-body text-primary-gold text-sm hover:underline">
                                Clear All Filters
                            </a>
                        </div>
                    <?php endif; ?>
                </div>
            </aside>
            
            <!-- RIGHT - PRODUCT GRID -->
            <main class="flex-1">
                <?php if (count($displayProducts) === 0): ?>
                    <!-- Empty State -->
                    <div class="bg-neutral-white rounded-2xl shadow-sm p-12 text-center">
                        <svg class="w-24 h-24 text-gray-300 mx-auto mb-4" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>
                            <path d="M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0-6 0" opacity="0.3"/>
                        </svg>
                        <h3 class="font-heading font-semibold text-charcoal text-lg mb-2">No products found</h3>
                        <p class="font-body text-gray-500 text-sm mb-6">Try adjusting your search or filters</p>
                        <a href="shop.php" class="inline-block bg-gradient-to-br from-primary-gold to-gold-light text-charcoal px-6 py-3 rounded-full font-heading font-semibold transition-transform hover:scale-105">
                            Clear Filters
                        </a>
                    </div>
                <?php else: ?>
                    <!-- Product Grid -->
                    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 mb-8">
                        <?php foreach ($displayProducts as $product): ?>
                            <div class="bg-neutral-white rounded-2xl shadow-sm hover:shadow-md transition-all hover:-translate-y-1 overflow-hidden">
                                <!-- Product Image -->
                                <a href="product.php?slug=<?php echo htmlspecialchars($product['slug']); ?>" class="block">
                                    <div class="relative aspect-square bg-gray-100 overflow-hidden">
                                        <img src="<?php echo htmlspecialchars($product['image']); ?>" 
                                             alt="<?php echo htmlspecialchars($product['name']); ?>" 
                                             class="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                                             onerror="this.src='/assets/product-images/SampleProduct.png'">
                                        
                                        <!-- Badges -->
                                        <?php if ($product['featured']): ?>
                                            <span class="absolute top-2 left-2 bg-accent-violet text-neutral-white text-xs font-body font-semibold px-3 py-1 rounded-full">
                                                Best Seller
                                            </span>
                                        <?php endif; ?>
                                        
                                        <?php if ($product['stock'] === 0): ?>
                                            <span class="absolute top-2 right-2 bg-red-500 text-neutral-white text-xs font-body font-semibold px-3 py-1 rounded-full">
                                                Out of Stock
                                            </span>
                                        <?php endif; ?>
                                    </div>
                                </a>
                                
                                <!-- Product Details -->
                                <div class="p-4">
                                    <!-- Product Name -->
                                    <a href="product.php?slug=<?php echo htmlspecialchars($product['slug']); ?>">
                                        <h3 class="font-heading font-semibold text-charcoal text-sm md:text-base line-clamp-2 mb-1 hover:text-primary-gold transition-colors">
                                            <?php echo htmlspecialchars($product['name']); ?>
                                        </h3>
                                    </a>
                                    
                                    <!-- Brand -->
                                    <p class="font-body text-gray-500 text-xs md:text-sm mb-2">
                                        <?php echo htmlspecialchars($product['brand']); ?>
                                    </p>
                                    
                                    <!-- Price -->
                                    <div class="mb-3">
                                        <span class="font-heading font-bold text-primary-gold text-lg">
                                            KSh <?php echo number_format($product['price']); ?>
                                        </span>
                                        <?php if ($product['compare_price']): ?>
                                            <span class="font-body text-gray-400 text-sm line-through ml-2">
                                                KSh <?php echo number_format($product['compare_price']); ?>
                                            </span>
                                        <?php endif; ?>
                                    </div>
                                    
                                    <!-- Rating -->
                                    <div class="flex items-center space-x-1 mb-4">
                                        <?php 
                                        $rating = $product['rating'];
                                        for ($i = 1; $i <= 5; $i++): 
                                            if ($i <= floor($rating)): ?>
                                                <svg class="w-4 h-4 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                                </svg>
                                            <?php elseif ($i - 0.5 <= $rating): ?>
                                                <svg class="w-4 h-4 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" opacity="0.5"/>
                                                </svg>
                                            <?php else: ?>
                                                <svg class="w-4 h-4 text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                                </svg>
                                            <?php endif;
                                        endfor; ?>
                                        <span class="font-body text-gray-500 text-xs ml-1">(<?php echo $product['review_count']; ?>)</span>
                                    </div>
                                    
                                    <!-- Action Buttons -->
                                    <div class="flex gap-2">
                                        <?php if ($product['stock'] > 0): ?>
                                            <button class="add-to-cart-btn flex-1 bg-gradient-to-br from-primary-gold to-gold-light text-charcoal font-heading font-semibold text-sm py-2 px-4 rounded-lg transition-transform hover:scale-105"
                                                    data-product-slug="<?php echo htmlspecialchars($product['slug']); ?>">
                                                Add to Cart
                                            </button>
                                            <a href="product.php?slug=<?php echo htmlspecialchars($product['slug']); ?>" 
                                               class="flex items-center justify-center px-3 py-2 border-2 border-primary-gold text-primary-gold rounded-lg hover:bg-primary-gold hover:text-charcoal transition-colors"
                                               title="View Details">
                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                                                </svg>
                                            </a>
                                        <?php else: ?>
                                            <button disabled class="w-full bg-gray-300 text-gray-500 font-heading font-semibold text-sm py-2 px-4 rounded-lg cursor-not-allowed">
                                                Out of Stock
                                            </button>
                                        <?php endif; ?>
                                    </div>
                                </div>
                            </div>
                        <?php endforeach; ?>
                    </div>
                    
                    <!-- 3. PAGINATION -->
                    <?php if ($totalPages > 1): ?>
                        <div class="flex items-center justify-center space-x-2">
                            <!-- Previous Button -->
                            <?php if ($currentPage > 1): ?>
                                <a href="<?php echo buildUrl(['page' => $currentPage - 1]); ?>" 
                                   class="px-4 py-2 border border-gray-300 rounded-lg font-body text-sm text-charcoal hover:bg-gray-50 transition-colors">
                                    Previous
                                </a>
                            <?php else: ?>
                                <span class="px-4 py-2 border border-gray-300 rounded-lg font-body text-sm text-gray-400 cursor-not-allowed">
                                    Previous
                                </span>
                            <?php endif; ?>
                            
                            <!-- Page Numbers -->
                            <?php
                            $pageRange = 2; // Show 2 pages before and after current
                            $startPage = max(1, $currentPage - $pageRange);
                            $endPage = min($totalPages, $currentPage + $pageRange);
                            
                            // Show first page if not in range
                            if ($startPage > 1): ?>
                                <a href="<?php echo buildUrl(['page' => 1]); ?>" 
                                   class="px-4 py-2 border border-gray-300 rounded-lg font-body text-sm text-charcoal hover:bg-gray-50 transition-colors">
                                    1
                                </a>
                                <?php if ($startPage > 2): ?>
                                    <span class="px-2 text-gray-400">...</span>
                                <?php endif;
                            endif;
                            
                            // Page number links
                            for ($i = $startPage; $i <= $endPage; $i++): 
                                if ($i === $currentPage): ?>
                                    <span class="px-4 py-2 bg-primary-gold text-charcoal rounded-lg font-body font-semibold text-sm">
                                        <?php echo $i; ?>
                                    </span>
                                <?php else: ?>
                                    <a href="<?php echo buildUrl(['page' => $i]); ?>" 
                                       class="px-4 py-2 border border-gray-300 rounded-lg font-body text-sm text-charcoal hover:bg-gray-50 transition-colors">
                                        <?php echo $i; ?>
                                    </a>
                                <?php endif;
                            endfor;
                            
                            // Show last page if not in range
                            if ($endPage < $totalPages): 
                                if ($endPage < $totalPages - 1): ?>
                                    <span class="px-2 text-gray-400">...</span>
                                <?php endif; ?>
                                <a href="<?php echo buildUrl(['page' => $totalPages]); ?>" 
                                   class="px-4 py-2 border border-gray-300 rounded-lg font-body text-sm text-charcoal hover:bg-gray-50 transition-colors">
                                    <?php echo $totalPages; ?>
                                </a>
                            <?php endif; ?>
                            
                            <!-- Next Button -->
                            <?php if ($currentPage < $totalPages): ?>
                                <a href="<?php echo buildUrl(['page' => $currentPage + 1]); ?>" 
                                   class="px-4 py-2 border border-gray-300 rounded-lg font-body text-sm text-charcoal hover:bg-gray-50 transition-colors">
                                    Next
                                </a>
                            <?php else: ?>
                                <span class="px-4 py-2 border border-gray-300 rounded-lg font-body text-sm text-gray-400 cursor-not-allowed">
                                    Next
                                </span>
                            <?php endif; ?>
                        </div>
                    <?php endif; ?>
                <?php endif; ?>
            </main>
        </div>
    </div>
</section>

</div>

<script>
// Build product lookup for cart.js
window.shopProductLookup = window.shopProductLookup || {};
<?php 
$productLookup = [];
foreach ($displayProducts as $p) {
    $productLookup[$p['slug']] = [
        'slug' => $p['slug'],
        'name' => $p['name'],
        'price' => $p['price'],
        'unit' => $p['unit'],
        'image' => $p['image']
    ];
}
echo 'Object.assign(window.shopProductLookup, ' . json_encode($productLookup) . ');';
?>
</script>

<?php include '../includes/footer.php'; ?>

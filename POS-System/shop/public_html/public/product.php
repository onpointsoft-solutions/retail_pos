<?php
require_once '../data/categories-data.php';
require_once '../data/products-data.php';

$slug = isset($_GET['slug']) ? $_GET['slug'] : null;
$product = $slug ? shop_get_product_by_slug($slug) : null;

// If no product found, redirect to shop.php
if (!$product) {
    header('Location: shop.php');
    exit;
}

$category = shop_get_category_by_slug($product['category']);
$relatedProducts = shop_get_related_products($slug, 4);

$pageTitle = $product['name'] . " - Victorious General Shop";
include '../includes/header.php';

// Calculate savings if compare price exists
$savings = $product['compare_price'] ? $product['compare_price'] - $product['price'] : 0;
?>

<!-- Alpine.js component data -->
<div x-data="{ 
    quantity: 1, 
    activeTab: 'description',
    wishlist: false,
    mainImage: <?php echo json_encode($product['image'], JSON_HEX_APOS | JSON_HEX_QUOT); ?>,
    incrementQty() { 
        if (this.quantity < <?php echo $product['stock']; ?>) this.quantity++ 
    },
    decrementQty() { 
        if (this.quantity > 1) this.quantity-- 
    },
    copyLink() {
        navigator.clipboard.writeText(window.location.href);
        alert('Link copied to clipboard!');
    }
}">

<!-- 1. BREADCRUMB BAND -->
<section class="bg-surface-gray py-4">
    <div class="container mx-auto px-4">
        <nav>
            <ol class="flex items-center space-x-2 text-sm font-body text-gray-500">
                <li><a href="index.php" class="hover:text-primary-gold">Home</a></li>
                <li>&gt;</li>
                <li><a href="shop.php" class="hover:text-primary-gold">Shop</a></li>
                <li>&gt;</li>
                <li>
                    <a href="shop.php?category=<?php echo htmlspecialchars($product['category']); ?>" class="hover:text-primary-gold">
                        <?php echo htmlspecialchars($category['name']); ?>
                    </a>
                </li>
                <li>&gt;</li>
                <li class="text-charcoal font-medium truncate"><?php echo htmlspecialchars($product['name']); ?></li>
            </ol>
        </nav>
    </div>
</section>

<!-- 2. MAIN PRODUCT SECTION -->
<section class="bg-surface-gray py-8">
    <div class="container mx-auto px-4">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-12">
            
            <!-- LEFT - Image Gallery -->
            <div>
                <!-- Main Image -->
                <div class="bg-neutral-white rounded-2xl overflow-hidden mb-4 relative">
                    <div class="aspect-square">
                        <img :src="mainImage" 
                             alt="<?php echo htmlspecialchars($product['name']); ?>" 
                             class="w-full h-full object-cover"
                             onerror="this.src='/assets/product-images/SampleProduct.png'">
                    </div>
                    
                    <!-- Out of Stock Badge -->
                    <?php if ($product['stock'] === 0): ?>
                        <div class="absolute top-4 left-4 bg-red-500 text-neutral-white px-4 py-2 rounded-full font-heading font-semibold text-sm">
                            Out of Stock
                        </div>
                    <?php endif; ?>
                </div>
                
                <!-- Thumbnail Row -->
                <div class="grid grid-cols-4 gap-3">
                    <?php foreach ($product['images'] as $image): ?>
                        <button @click="mainImage = <?php echo htmlspecialchars(json_encode($image), ENT_QUOTES, 'UTF-8'); ?>"
                                class="bg-neutral-white rounded-lg overflow-hidden border-2 hover:border-primary-gold transition-colors"
                                :class="{ 'border-primary-gold': mainImage === <?php echo htmlspecialchars(json_encode($image), ENT_QUOTES, 'UTF-8'); ?> }">
                            <div class="aspect-square">
                                <img src="<?php echo htmlspecialchars($image); ?>"
                                     alt="<?php echo htmlspecialchars($product['name']); ?>" 
                                     class="w-full h-full object-cover"
                                     onerror="this.src='/assets/product-images/SampleProduct.png'">
                            </div>
                        </button>
                    <?php endforeach; ?>
                </div>
            </div>
            
            <!-- RIGHT - Product Info -->
            <div>
                <!-- Brand -->
                <p class="font-body text-gray-500 text-xs uppercase tracking-wide mb-2">
                    <?php echo htmlspecialchars($product['brand']); ?>
                </p>
                
                <!-- Product Name -->
                <h1 class="font-heading font-bold text-charcoal text-3xl mb-4">
                    <?php echo htmlspecialchars($product['name']); ?>
                </h1>
                
                <!-- Star Rating -->
                <div class="flex items-center space-x-2 mb-4">
                    <div class="flex items-center space-x-1">
                        <?php 
                        $rating = $product['rating'];
                        for ($i = 1; $i <= 5; $i++): 
                            if ($i <= floor($rating)): ?>
                                <svg class="w-5 h-5 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                </svg>
                            <?php elseif ($i - 0.5 <= $rating): ?>
                                <svg class="w-5 h-5 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" opacity="0.5"/>
                                </svg>
                            <?php else: ?>
                                <svg class="w-5 h-5 text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                </svg>
                            <?php endif;
                        endfor; ?>
                    </div>
                    <span class="font-body text-gray-500 text-sm">
                        <?php echo number_format($rating, 1); ?> (<?php echo $product['review_count']; ?> reviews)
                    </span>
                </div>
                
                <!-- Price -->
                <div class="flex items-center space-x-3 mb-4">
                    <span class="font-heading font-bold text-primary-gold text-4xl">
                        KSh <?php echo number_format($product['price']); ?>
                    </span>
                    <?php if ($product['compare_price']): ?>
                        <span class="font-body text-gray-400 text-xl line-through">
                            KSh <?php echo number_format($product['compare_price']); ?>
                        </span>
                        <span class="bg-primary-gold text-charcoal px-3 py-1 rounded-full font-body font-semibold text-sm">
                            Save KSh <?php echo number_format($savings); ?>
                        </span>
                    <?php endif; ?>
                </div>
                
                <!-- Stock Status -->
                <div class="flex items-center space-x-2 mb-4">
                    <?php if ($product['stock'] > 0): ?>
                        <span class="w-3 h-3 bg-green-500 rounded-full"></span>
                        <span class="font-body text-charcoal text-sm">
                            <?php if ($product['stock'] <= 20): ?>
                                In Stock (<?php echo $product['stock']; ?> available)
                            <?php else: ?>
                                In Stock
                            <?php endif; ?>
                        </span>
                    <?php else: ?>
                        <span class="w-3 h-3 bg-red-500 rounded-full"></span>
                        <span class="font-body text-red-500 text-sm">Out of Stock</span>
                    <?php endif; ?>
                </div>
                
                <!-- Unit/Size -->
                <div class="mb-6">
                    <span class="font-body text-charcoal text-sm">
                        <span class="font-semibold">Size:</span> <?php echo htmlspecialchars($product['unit']); ?>
                    </span>
                </div>
                
                <!-- Quantity Selector -->
                <div class="mb-6">
                    <label class="font-body font-semibold text-charcoal text-sm mb-2 block">Quantity</label>
                    <div class="flex items-center space-x-3">
                        <button @click="decrementQty()" 
                                <?php echo $product['stock'] === 0 ? 'disabled' : ''; ?>
                                class="w-10 h-10 border-2 border-gray-300 rounded-lg flex items-center justify-center hover:border-primary-gold transition-colors <?php echo $product['stock'] === 0 ? 'opacity-50 cursor-not-allowed' : ''; ?>">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/>
                            </svg>
                        </button>
                        <input type="number" 
                               x-model="quantity" 
                               min="1" 
                               max="<?php echo $product['stock']; ?>"
                               <?php echo $product['stock'] === 0 ? 'disabled' : ''; ?>
                               class="w-20 h-10 text-center border-2 border-gray-300 rounded-lg focus:outline-none focus:border-primary-gold <?php echo $product['stock'] === 0 ? 'opacity-50 cursor-not-allowed' : ''; ?>">
                        <button @click="incrementQty()" 
                                <?php echo $product['stock'] === 0 ? 'disabled' : ''; ?>
                                class="w-10 h-10 border-2 border-gray-300 rounded-lg flex items-center justify-center hover:border-primary-gold transition-colors <?php echo $product['stock'] === 0 ? 'opacity-50 cursor-not-allowed' : ''; ?>">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                            </svg>
                        </button>
                    </div>
                </div>
                
                <!-- Action Buttons -->
                <div class="flex flex-col sm:flex-row gap-3 mb-6">
                    <?php if ($product['stock'] > 0): ?>
                        <button class="add-to-cart-btn flex-1 bg-gradient-to-br from-primary-gold to-gold-light text-charcoal font-heading font-semibold py-3 px-6 rounded-lg transition-transform hover:scale-105"
                                data-product-slug="<?php echo htmlspecialchars($product['slug']); ?>">
                            Add to Cart
                        </button>
                        <button class="buy-now-btn flex-1 bg-charcoal text-neutral-white font-heading font-semibold py-3 px-6 rounded-lg transition-transform hover:scale-105"
                                data-product-slug="<?php echo htmlspecialchars($product['slug']); ?>">
                            Buy Now
                        </button>
                    <?php else: ?>
                        <button disabled class="w-full bg-gray-300 text-gray-500 font-heading font-semibold py-3 px-6 rounded-lg cursor-not-allowed">
                            Out of Stock
                        </button>
                    <?php endif; ?>
                </div>
                
                <!-- Wishlist Button -->
                <div class="mb-6">
                    <button @click="wishlist = !wishlist" 
                            class="flex items-center space-x-2 font-body text-sm transition-colors"
                            :class="wishlist ? 'text-accent-violet' : 'text-gray-500 hover:text-accent-violet'">
                        <svg class="w-6 h-6" :fill="wishlist ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                        </svg>
                        <span x-text="wishlist ? 'Added to Wishlist' : 'Add to Wishlist'"></span>
                    </button>
                </div>
                
                <!-- Description -->
                <div class="mb-6">
                    <p class="font-body text-charcoal text-base leading-relaxed">
                        <?php echo htmlspecialchars($product['description']); ?>
                    </p>
                </div>
                
                <!-- Delivery Info -->
                <div class="bg-surface-gray rounded-lg p-4 mb-6">
                    <div class="flex items-start space-x-3">
                        <svg class="w-6 h-6 text-primary-gold flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4"/>
                        </svg>
                        <p class="font-body text-gray-600 text-sm">
                            Delivered within 1-3 days in Nairobi. Delivery fees calculated at checkout.
                        </p>
                    </div>
                </div>
                
                <!-- Share This Product -->
                <div>
                    <p class="font-body font-semibold text-charcoal text-sm mb-3">Share this product</p>
                    <div class="flex items-center space-x-3">
                        <a href="https://wa.me/?text=Check%20out%20<?php echo urlencode($product['name']); ?>%20at%20<?php echo urlencode($_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI']); ?>" 
                           target="_blank"
                           class="flex items-center justify-center w-10 h-10 bg-[#25D366] text-neutral-white rounded-full hover:scale-110 transition-transform">
                            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413Z"/>
                            </svg>
                        </a>
                        <button @click="copyLink()" 
                                class="flex items-center justify-center w-10 h-10 bg-gray-200 text-charcoal rounded-full hover:scale-110 transition-transform">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 3. PRODUCT DETAILS TABS -->
        <div class="bg-neutral-white rounded-2xl shadow-sm overflow-hidden mb-12">
            <!-- Tab Headers -->
            <div class="border-b border-gray-200">
                <div class="flex">
                    <button @click="activeTab = 'description'" 
                            class="flex-1 py-4 px-6 font-heading font-semibold text-sm transition-colors"
                            :class="activeTab === 'description' ? 'text-primary-gold border-b-2 border-primary-gold' : 'text-gray-500 hover:text-charcoal'">
                        Description
                    </button>
                    <button @click="activeTab = 'specifications'" 
                            class="flex-1 py-4 px-6 font-heading font-semibold text-sm transition-colors"
                            :class="activeTab === 'specifications' ? 'text-primary-gold border-b-2 border-primary-gold' : 'text-gray-500 hover:text-charcoal'">
                        Specifications
                    </button>
                    <button @click="activeTab = 'reviews'" 
                            class="flex-1 py-4 px-6 font-heading font-semibold text-sm transition-colors"
                            :class="activeTab === 'reviews' ? 'text-primary-gold border-b-2 border-primary-gold' : 'text-gray-500 hover:text-charcoal'">
                        Reviews
                    </button>
                </div>
            </div>
            
            <!-- Tab Content -->
            <div class="p-6">
                <!-- Description Tab -->
                <div x-show="activeTab === 'description'" x-transition>
                    <h3 class="font-heading font-semibold text-charcoal text-lg mb-4">Product Description</h3>
                    <p class="font-body text-charcoal text-base leading-relaxed mb-4">
                        <?php echo htmlspecialchars($product['description']); ?>
                    </p>
                    <ul class="space-y-2 font-body text-charcoal text-sm">
                        <li>• High quality product from trusted <?php echo htmlspecialchars($product['brand']); ?> brand</li>
                        <li>• Suitable for everyday use in Kenyan households</li>
                        <li>• Great value for money with reliable performance</li>
                        <li>• Safe and tested for quality assurance</li>
                    </ul>
                </div>
                
                <!-- Specifications Tab -->
                <div x-show="activeTab === 'specifications'" x-transition>
                    <h3 class="font-heading font-semibold text-charcoal text-lg mb-4">Product Specifications</h3>
                    <div class="space-y-3">
                        <div class="flex border-b border-gray-200 pb-2">
                            <span class="font-body font-semibold text-charcoal text-sm w-1/3">Brand</span>
                            <span class="font-body text-gray-600 text-sm"><?php echo htmlspecialchars($product['brand']); ?></span>
                        </div>
                        <div class="flex border-b border-gray-200 pb-2">
                            <span class="font-body font-semibold text-charcoal text-sm w-1/3">Category</span>
                            <span class="font-body text-gray-600 text-sm"><?php echo htmlspecialchars($category['name']); ?></span>
                        </div>
                        <div class="flex border-b border-gray-200 pb-2">
                            <span class="font-body font-semibold text-charcoal text-sm w-1/3">Unit Size</span>
                            <span class="font-body text-gray-600 text-sm"><?php echo htmlspecialchars($product['unit']); ?></span>
                        </div>
                        <div class="flex border-b border-gray-200 pb-2">
                            <span class="font-body font-semibold text-charcoal text-sm w-1/3">SKU</span>
                            <span class="font-body text-gray-600 text-sm uppercase"><?php echo htmlspecialchars($product['slug']); ?></span>
                        </div>
                        <div class="flex border-b border-gray-200 pb-2">
                            <span class="font-body font-semibold text-charcoal text-sm w-1/3">Availability</span>
                            <span class="font-body text-gray-600 text-sm">
                                <?php echo $product['stock'] > 0 ? 'In Stock' : 'Out of Stock'; ?>
                            </span>
                        </div>
                        <div class="flex pb-2">
                            <span class="font-body font-semibold text-charcoal text-sm w-1/3">Rating</span>
                            <span class="font-body text-gray-600 text-sm">
                                <?php echo number_format($product['rating'], 1); ?>/5.0 (<?php echo $product['review_count']; ?> reviews)
                            </span>
                        </div>
                    </div>
                </div>
                
                <!-- Reviews Tab -->
                <div x-show="activeTab === 'reviews'" x-transition>
                    <h3 class="font-heading font-semibold text-charcoal text-lg mb-4">Customer Reviews</h3>
                    <div class="space-y-4">
                        <!-- Review 1 -->
                        <div class="border-b border-gray-200 pb-4">
                            <div class="flex items-start justify-between mb-2">
                                <div>
                                    <p class="font-body font-semibold text-charcoal text-sm">Jane Muthoni</p>
                                    <p class="font-body text-gray-500 text-xs">Verified Purchase • 2 weeks ago</p>
                                </div>
                                <div class="flex items-center space-x-1">
                                    <?php for ($i = 0; $i < 5; $i++): ?>
                                        <svg class="w-4 h-4 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                        </svg>
                                    <?php endfor; ?>
                                </div>
                            </div>
                            <p class="font-body text-charcoal text-sm">
                                Excellent product! Delivery was quick and the quality is exactly as described. Will definitely order again.
                            </p>
                        </div>
                        
                        <!-- Review 2 -->
                        <div class="border-b border-gray-200 pb-4">
                            <div class="flex items-start justify-between mb-2">
                                <div>
                                    <p class="font-body font-semibold text-charcoal text-sm">Peter Kamau</p>
                                    <p class="font-body text-gray-500 text-xs">Verified Purchase • 3 weeks ago</p>
                                </div>
                                <div class="flex items-center space-x-1">
                                    <?php for ($i = 0; $i < 4; $i++): ?>
                                        <svg class="w-4 h-4 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                        </svg>
                                    <?php endfor; ?>
                                    <svg class="w-4 h-4 text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                    </svg>
                                </div>
                            </div>
                            <p class="font-body text-charcoal text-sm">
                                Good value for the price. The packaging could be better but the product itself is great.
                            </p>
                        </div>
                        
                        <!-- Review 3 -->
                        <div class="pb-4">
                            <div class="flex items-start justify-between mb-2">
                                <div>
                                    <p class="font-body font-semibold text-charcoal text-sm">Mary Achieng</p>
                                    <p class="font-body text-gray-500 text-xs">Verified Purchase • 1 month ago</p>
                                </div>
                                <div class="flex items-center space-x-1">
                                    <?php for ($i = 0; $i < 5; $i++): ?>
                                        <svg class="w-4 h-4 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                        </svg>
                                    <?php endfor; ?>
                                </div>
                            </div>
                            <p class="font-body text-charcoal text-sm">
                                Very happy with my purchase! The shop has genuine products and the customer service is excellent. Highly recommend.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- 4. RELATED PRODUCTS -->
<?php if (count($relatedProducts) > 0): ?>
<section class="bg-surface-gray py-12">
    <div class="container mx-auto px-4">
        <h2 class="text-center font-heading font-bold text-charcoal text-3xl mb-8">
            You May Also Like
        </h2>
        
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
            <?php foreach ($relatedProducts as $relatedProduct): ?>
                <div class="bg-neutral-white rounded-2xl shadow-sm hover:shadow-md transition-all hover:-translate-y-1 overflow-hidden">
                    <!-- Product Image -->
                    <a href="product.php?slug=<?php echo htmlspecialchars($relatedProduct['slug']); ?>" class="block">
                        <div class="relative aspect-square bg-gray-100 overflow-hidden">
                            <img src="<?php echo htmlspecialchars($relatedProduct['image']); ?>" 
                                 alt="<?php echo htmlspecialchars($relatedProduct['name']); ?>" 
                                 class="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                                 onerror="this.src='/assets/product-images/SampleProduct.png'">
                            
                            <?php if ($relatedProduct['featured']): ?>
                                <span class="absolute top-2 left-2 bg-accent-violet text-neutral-white text-xs font-body font-semibold px-3 py-1 rounded-full">
                                    Best Seller
                                </span>
                            <?php endif; ?>
                            
                            <?php if ($relatedProduct['stock'] === 0): ?>
                                <span class="absolute top-2 right-2 bg-red-500 text-neutral-white text-xs font-body font-semibold px-3 py-1 rounded-full">
                                    Out of Stock
                                </span>
                            <?php endif; ?>
                        </div>
                    </a>
                    
                    <!-- Product Details -->
                    <div class="p-4">
                        <a href="product.php?slug=<?php echo htmlspecialchars($relatedProduct['slug']); ?>">
                            <h3 class="font-heading font-semibold text-charcoal text-sm md:text-base line-clamp-2 mb-1 hover:text-primary-gold transition-colors">
                                <?php echo htmlspecialchars($relatedProduct['name']); ?>
                            </h3>
                        </a>
                        
                        <p class="font-body text-gray-500 text-xs md:text-sm mb-2">
                            <?php echo htmlspecialchars($relatedProduct['brand']); ?>
                        </p>
                        
                        <div class="mb-3">
                            <span class="font-heading font-bold text-primary-gold text-lg">
                                KSh <?php echo number_format($relatedProduct['price']); ?>
                            </span>
                            <?php if ($relatedProduct['compare_price']): ?>
                                <span class="font-body text-gray-400 text-sm line-through ml-2">
                                    KSh <?php echo number_format($relatedProduct['compare_price']); ?>
                                </span>
                            <?php endif; ?>
                        </div>
                        
                        <div class="flex items-center space-x-1 mb-4">
                            <?php 
                            $relatedRating = $relatedProduct['rating'];
                            for ($i = 1; $i <= 5; $i++): 
                                if ($i <= floor($relatedRating)): ?>
                                    <svg class="w-4 h-4 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                    </svg>
                                <?php elseif ($i - 0.5 <= $relatedRating): ?>
                                    <svg class="w-4 h-4 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" opacity="0.5"/>
                                    </svg>
                                <?php else: ?>
                                    <svg class="w-4 h-4 text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                    </svg>
                                <?php endif;
                            endfor; ?>
                            <span class="font-body text-gray-500 text-xs ml-1">(<?php echo $relatedProduct['review_count']; ?>)</span>
                        </div>
                        
                        <?php if ($relatedProduct['stock'] > 0): ?>
                            <a href="product.php?slug=<?php echo htmlspecialchars($relatedProduct['slug']); ?>" 
                               class="block w-full bg-gradient-to-br from-primary-gold to-gold-light text-charcoal font-heading font-semibold text-sm py-2 px-4 rounded-lg transition-transform hover:scale-105 text-center">
                                View Details
                            </a>
                        <?php else: ?>
                            <button disabled class="w-full bg-gray-300 text-gray-500 font-heading font-semibold text-sm py-2 px-4 rounded-lg cursor-not-allowed">
                                Out of Stock
                            </button>
                        <?php endif; ?>
                    </div>
                </div>
            <?php endforeach; ?>
        </div>
    </div>
</section>
<?php endif; ?>

</div>

<script>
// Build product lookup for cart.js
window.shopProductLookup = window.shopProductLookup || {};
<?php 
$productLookup = [];

// Add main product
$productLookup[$product['slug']] = [
    'slug' => $product['slug'],
    'name' => $product['name'],
    'price' => $product['price'],
    'unit' => $product['unit'],
    'image' => $product['image']
];

// Add related products
foreach ($relatedProducts as $p) {
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

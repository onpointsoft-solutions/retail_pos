<?php
require_once '../data/categories-data.php';
require_once '../data/products-data.php';

$pageTitle = "Victorious General Shop - Your Neighborhood Shop, Now Online";
include '../includes/header.php';

$categories = shop_get_categories();
$featuredProducts = shop_get_featured_products(8);
$allProducts = shop_get_products();
$newArrivals = array_slice($allProducts, -4, 4);
?>

<!-- 1. HERO BANNER -->
<section class="relative min-h-[500px] flex items-center overflow-hidden">
    <!-- Background Gradient -->
    <div class="absolute inset-0 bg-gradient-to-br from-primary-gold to-gold-light"></div>
    
    <!-- Hero Image - positioned right on desktop, full background on mobile -->
    <div class="absolute right-0 top-0 bottom-0 w-full md:w-1/2">
        <!-- Mobile overlay -->
        <div class="absolute inset-0 md:hidden bg-charcoal opacity-40 z-10"></div>
        
        <!-- Image -->
        <img src="<?php echo !empty($featuredProducts[0]['image']) ? $featuredProducts[0]['image'] : '/assets/product-images/SampleProduct.png'; ?>" 
             alt="Victorious General Shop Products" 
             class="w-full h-full object-cover opacity-80 md:opacity-100"
             onerror="this.src='/assets/product-images/SampleProduct.png'">
        
        <!-- Desktop gradient overlay for blending -->
        <div class="hidden md:block absolute inset-0 bg-gradient-to-l from-transparent to-primary-gold opacity-30"></div>
    </div>
    
    <!-- Content -->
    <div class="container mx-auto px-4 py-16 md:py-20 relative z-10">
        <div class="max-w-xl">
            <!-- Badge -->
            <div class="inline-block bg-neutral-white bg-opacity-90 text-charcoal px-4 py-2 rounded-full mb-6">
                <span class="font-body font-medium text-sm">🛒 Now Delivering Across Nairobi</span>
            </div>
            
            <!-- Heading -->
            <h1 class="font-heading font-bold text-4xl md:text-5xl mb-4">
                <span class="text-neutral-white">Everything Your Home Needs,</span><br>
                <span class="text-charcoal">Delivered to Your Door.</span>
            </h1>
            
            <!-- Subtext -->
            <p class="font-body text-neutral-white text-lg mb-8">
                From groceries to household essentials — shop Victorious General Shop online and get it delivered fast.
            </p>
            
            <!-- Buttons -->
            <div class="flex flex-col sm:flex-row gap-4">
                <a href="shop.php" class="inline-block bg-charcoal text-neutral-white px-8 py-3 rounded-full font-heading font-semibold text-center transition-transform hover:scale-105">
                    Shop Now →
                </a>
                <a href="https://wa.me/254742071810?text=Hi%20Victorious%20General%20Shop%2C%20I%27d%20like%20to%20ask%20about%20a%20product" 
                   target="_blank" 
                   class="inline-block border-2 border-neutral-white text-neutral-white px-8 py-3 rounded-full font-heading font-semibold text-center transition-all hover:bg-neutral-white hover:text-charcoal">
                    Chat on WhatsApp
                </a>
            </div>
        </div>
    </div>
</section>

<!-- 2. TRUST STRIP -->
<section class="bg-surface-gray py-8">
    <div class="container mx-auto px-4">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div class="flex items-center justify-center space-x-3">
                <span class="text-3xl text-accent-violet">🚚</span>
                <span class="font-heading font-semibold text-charcoal text-sm">Fast Delivery</span>
            </div>
            <div class="flex items-center justify-center space-x-3">
                <span class="text-3xl text-accent-violet">💳</span>
                <span class="font-heading font-semibold text-charcoal text-sm">Secure Payments</span>
            </div>
            <div class="flex items-center justify-center space-x-3">
                <span class="text-3xl text-accent-violet">✅</span>
                <span class="font-heading font-semibold text-charcoal text-sm">Quality Guaranteed</span>
            </div>
            <div class="flex items-center justify-center space-x-3">
                <span class="text-3xl text-accent-violet">📞</span>
                <span class="font-heading font-semibold text-charcoal text-sm">Friendly Support</span>
            </div>
        </div>
    </div>
</section>

<!-- SHOP FILTERS -->
<section class="bg-white border-y border-gray-200 sticky top-0 z-30">
    <div class="container mx-auto px-4 py-4">

        <div class="flex flex-col lg:flex-row lg:items-center gap-4">

            <!-- Search -->
            <div class="relative flex-1">
                <svg xmlns="http://www.w3.org/2000/svg"
                     class="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2"
                     fill="none"
                     viewBox="0 0 24 24"
                     stroke="currentColor">
                    <path stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z"/>
                </svg>

                <input
                    type="text"
                    id="searchProducts"
                    placeholder="Search products..."
                    class="w-full h-12 rounded-full border border-gray-300 bg-gray-50 pl-12 pr-4 focus:outline-none focus:ring-2 focus:ring-primary-gold focus:border-primary-gold">
            </div>

            <!-- Category Filters -->
            <div class="flex items-center gap-2 overflow-x-auto scrollbar-hide whitespace-nowrap">

                <!-- All -->
                <a href="shop.php"
                   class="px-5 py-2 rounded-full bg-primary-gold text-white text-sm font-medium hover:opacity-90 transition">
                    All
                </a>

                <?php foreach($categories as $category): ?>
                    <a href="shop.php?category=<?php echo htmlspecialchars($category['slug']); ?>"
                       class="flex items-center gap-2 px-4 py-2 rounded-full border border-gray-300 bg-white hover:border-primary-gold hover:text-primary-gold transition text-sm font-medium">

                        <span><?php echo $category['icon']; ?></span>

                        <span>
                            <?php echo htmlspecialchars($category['name']); ?>
                        </span>

                    </a>
                <?php endforeach; ?>

            </div>

        </div>

    </div>
</section>
<!-- 4. FEATURED / BESTSELLING PRODUCTS -->
<section class="bg-surface-gray py-16">
    <div class="container mx-auto px-4">
        <!-- Section Label -->
        <p class="text-center text-primary-gold font-body font-semibold text-sm uppercase tracking-wide mb-3">
            CUSTOMER FAVORITES
        </p>
        
        <!-- Heading -->
        <h2 class="text-center font-heading font-bold text-charcoal text-3xl md:text-4xl mb-12">
            Bestselling Products
        </h2>
        
        <!-- Product Grid -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
            <?php foreach ($featuredProducts as $product): ?>
                <div class="bg-neutral-white rounded-2xl shadow-sm hover:shadow-md transition-all hover:-translate-y-1 overflow-hidden">
                    <!-- Product Image -->
                    <div class="relative aspect-square bg-gray-100 rounded-t-2xl overflow-hidden">
                        <!-- Product Image -->
                        <img src="<?php echo htmlspecialchars($product['image']); ?>" 
                             alt="<?php echo htmlspecialchars($product['name']); ?>" 
                             class="w-full h-full object-cover"
                             onerror="this.src='/assets/product-images/SampleProduct.png'">
                        
                        <!-- Best Seller Badge -->
                        <?php if ($product['featured']): ?>
                            <span class="absolute top-2 left-2 bg-accent-violet text-neutral-white text-xs font-body font-semibold px-3 py-1 rounded-full">
                                Best Seller
                            </span>
                        <?php endif; ?>
                        
                        <!-- Out of Stock Tag -->
                        <?php if ($product['stock'] === 0): ?>
                            <span class="absolute top-2 right-2 bg-red-500 text-neutral-white text-xs font-body font-semibold px-3 py-1 rounded-full">
                                Out of Stock
                            </span>
                        <?php endif; ?>
                    </div>
                    
                    <!-- Product Details -->
                    <div class="p-4">
                        <!-- Product Name -->
                        <h3 class="font-heading font-semibold text-charcoal text-sm md:text-base line-clamp-2 mb-1">
                            <?php echo htmlspecialchars($product['name']); ?>
                        </h3>
                        
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
        
        <!-- View All Button -->
        <div class="text-center">
            <a href="shop.php" class="inline-block border-2 border-primary-gold text-primary-gold px-8 py-3 rounded-full font-heading font-semibold transition-all hover:bg-primary-gold hover:text-charcoal">
                View All Products →
            </a>
        </div>
    </div>
</section>

<!-- 5. PROMOTIONAL BANNER -->
<section class="bg-charcoal py-16">
    <div class="container mx-auto px-4 text-center">
        <h2 class="font-heading font-bold text-neutral-white text-2xl md:text-3xl mb-4">
            Weekend Special — Up to 20% Off Selected Items
        </h2>
        <p class="font-body text-gray-300 text-base md:text-lg mb-6">
            Limited time only. Stock up on your household essentials today.
        </p>
        <a href="shop.php" class="inline-block bg-gradient-to-br from-primary-gold to-gold-light text-charcoal px-8 py-3 rounded-full font-heading font-semibold transition-transform hover:scale-105">
            Shop the Deals →
        </a>
    </div>
</section>

<!-- 6. NEW ARRIVALS -->
<section class="bg-surface-gray py-16">
    <div class="container mx-auto px-4">
        <!-- Section Label -->
        <p class="text-center text-primary-gold font-body font-semibold text-sm uppercase tracking-wide mb-3">
            JUST IN
        </p>
        
        <!-- Heading -->
        <h2 class="text-center font-heading font-bold text-charcoal text-3xl md:text-4xl mb-12">
            New Arrivals
        </h2>
        
        <!-- Product Grid -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
            <?php foreach ($newArrivals as $product): ?>
                <div class="bg-neutral-white rounded-2xl shadow-sm hover:shadow-md transition-all hover:-translate-y-1 overflow-hidden">
                    <!-- Product Image -->
                    <div class="relative aspect-square bg-gray-100 rounded-t-2xl overflow-hidden">
                        <img src="<?php echo htmlspecialchars($product['image']); ?>" 
                             alt="<?php echo htmlspecialchars($product['name']); ?>" 
                             class="w-full h-full object-cover"
                             onerror="this.src='/assets/product-images/SampleProduct.png'">
                        
                        <?php if ($product['stock'] === 0): ?>
                            <span class="absolute top-2 right-2 bg-red-500 text-neutral-white text-xs font-body font-semibold px-3 py-1 rounded-full">
                                Out of Stock
                            </span>
                        <?php endif; ?>
                    </div>
                    
                    <!-- Product Details -->
                    <div class="p-4">
                        <h3 class="font-heading font-semibold text-charcoal text-sm md:text-base line-clamp-2 mb-1">
                            <?php echo htmlspecialchars($product['name']); ?>
                        </h3>
                        
                        <p class="font-body text-gray-500 text-xs md:text-sm mb-2">
                            <?php echo htmlspecialchars($product['brand']); ?>
                        </p>
                        
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
                        
                        <?php if ($product['stock'] > 0): ?>
                            <button class="w-full bg-gradient-to-br from-primary-gold to-gold-light text-charcoal font-heading font-semibold text-sm py-2 px-4 rounded-lg transition-transform hover:scale-105">
                                Add to Cart
                            </button>
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

<!-- 7. WHY SHOP WITH US -->
<section class="bg-surface-gray py-16">
    <div class="container mx-auto px-4">
        <h2 class="text-center font-heading font-bold text-charcoal text-2xl md:text-3xl mb-12">
            Why Shop With Victorious General Shop
        </h2>
        
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
            <!-- Feature 1 -->
            <div class="text-center">
                <div class="w-16 h-16 bg-accent-violet bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg class="w-8 h-8 text-accent-violet" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M4 4h16v2H4V4zm0 4h16v2H4V8zm0 4h16v2H4v-2zm0 4h16v2H4v-2zm0 4h16v2H4v-2z"/>
                    </svg>
                </div>
                <h3 class="font-heading font-semibold text-charcoal text-lg mb-3">Wide Selection</h3>
                <p class="font-body text-gray-600 text-sm">
                    Thousands of products across groceries, household goods, and more, all in one place.
                </p>
            </div>
            
            <!-- Feature 2 -->
            <div class="text-center">
                <div class="w-16 h-16 bg-accent-violet bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg class="w-8 h-8 text-accent-violet" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M3 3h18v2H3V3zm0 4h18v2H3V7zm0 4h18v2H3v-2zm0 4h18v2H3v-2zm0 4h18v2H3v-2z"/>
                        <path d="M8 12l3 3 5-5"/>
                    </svg>
                </div>
                <h3 class="font-heading font-semibold text-charcoal text-lg mb-3">Fast, Reliable Delivery</h3>
                <p class="font-body text-gray-600 text-sm">
                    We get your order to you quickly, packed with care every time.
                </p>
            </div>
            
            <!-- Feature 3 -->
            <div class="text-center">
                <div class="w-16 h-16 bg-accent-violet bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg class="w-8 h-8 text-accent-violet" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/>
                    </svg>
                </div>
                <h3 class="font-heading font-semibold text-charcoal text-lg mb-3">Real People, Real Support</h3>
                <p class="font-body text-gray-600 text-sm">
                    Reach us anytime on WhatsApp — we're a real shop, not a faceless warehouse.
                </p>
            </div>
        </div>
    </div>
</section>

<!-- 8. TESTIMONIALS -->
<section class="bg-surface-gray py-16">
    <div class="container mx-auto px-4">
        <h2 class="text-center font-heading font-bold text-charcoal text-2xl md:text-3xl mb-12">
            What Our Customers Say
        </h2>
        
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
            <!-- Testimonial 1 -->
            <div class="bg-neutral-white rounded-2xl p-6 border-l-4 border-primary-gold">
                <svg class="w-10 h-10 text-primary-gold mb-4" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/>
                </svg>
                <p class="font-body italic text-charcoal text-sm md:text-base mb-4">
                    "I ordered maize flour and cooking oil on Saturday morning and it arrived by afternoon. The delivery guy was so polite and helpful. Will definitely order again!"
                </p>
                <p class="font-body font-bold text-charcoal text-sm">Grace Wanjiru</p>
                <p class="font-body text-gray-500 text-xs">Kasarani, Nairobi</p>
            </div>
            
            <!-- Testimonial 2 -->
            <div class="bg-neutral-white rounded-2xl p-6 border-l-4 border-primary-gold">
                <svg class="w-10 h-10 text-primary-gold mb-4" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/>
                </svg>
                <p class="font-body italic text-charcoal text-sm md:text-base mb-4">
                    "The quality of products is exactly what I get when I walk into their physical shop. Fresh milk, good brands, nothing expired. Very trustworthy."
                </p>
                <p class="font-body font-bold text-charcoal text-sm">James Omondi</p>
                <p class="font-body text-gray-500 text-xs">Donholm, Nairobi</p>
            </div>
            
            <!-- Testimonial 3 -->
            <div class="bg-neutral-white rounded-2xl p-6 border-l-4 border-primary-gold">
                <svg class="w-10 h-10 text-primary-gold mb-4" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/>
                </svg>
                <p class="font-body italic text-charcoal text-sm md:text-base mb-4">
                    "I just message them on WhatsApp and they sort me out immediately. No complicated apps or confusing checkout. Simple and convenient!"
                </p>
                <p class="font-body font-bold text-charcoal text-sm">Mary Akinyi</p>
                <p class="font-body text-gray-500 text-xs">Umoja, Nairobi</p>
            </div>
        </div>
    </div>
</section>

<!-- 9. NEWSLETTER SIGNUP -->
<section class="bg-gradient-to-br from-primary-gold to-gold-light py-16">
    <div class="container mx-auto px-4 text-center">
        <h2 class="font-heading font-bold text-charcoal text-2xl md:text-3xl mb-3">
            Never Miss a Deal
        </h2>
        <p class="font-body text-charcoal text-sm md:text-base mb-8">
            Get notified about new arrivals and weekend specials.
        </p>
        
        <form class="max-w-md mx-auto flex flex-col sm:flex-row gap-3">
            <input type="email" 
                   placeholder="Enter your email address" 
                   class="flex-1 px-6 py-3 rounded-full border-0 focus:outline-none focus:ring-2 focus:ring-charcoal font-body text-sm"
                   required>
            <button type="submit" 
                    class="bg-charcoal text-neutral-white px-8 py-3 rounded-full font-heading font-semibold text-sm transition-transform hover:scale-105">
                Subscribe →
            </button>
        </form>
    </div>
</section>

<!-- 10. CLOSING CTA -->
<section class="bg-charcoal py-16">
    <div class="container mx-auto px-4 text-center">
        <h2 class="font-heading font-bold text-neutral-white text-2xl md:text-3xl mb-8">
            Ready to Shop?
        </h2>
        
        <div class="flex flex-col sm:flex-row justify-center gap-4">
            <a href="shop.php" class="inline-block bg-gradient-to-br from-primary-gold to-gold-light text-charcoal px-8 py-3 rounded-full font-heading font-semibold transition-transform hover:scale-105">
                Browse All Products
            </a>
            <a href="https://wa.me/254742071810?text=Hi%20Victorious%20General%20Shop%2C%20I%27d%20like%20to%20ask%20about%20a%20product" 
               target="_blank" 
               class="inline-block border-2 border-neutral-white text-neutral-white px-8 py-3 rounded-full font-heading font-semibold transition-all hover:bg-neutral-white hover:text-charcoal">
                Message Us on WhatsApp
            </a>
        </div>
    </div>
</section>

<script>
// Build product lookup for cart.js (homepage featured and new arrivals)
window.shopProductLookup = window.shopProductLookup || {};
<?php 
$productLookup = [];

// Add featured products
foreach ($featuredProducts as $p) {
    $productLookup[$p['slug']] = [
        'slug' => $p['slug'],
        'name' => $p['name'],
        'price' => $p['price'],
        'unit' => $p['unit'],
        'image' => $p['image']
    ];
}

// Add new arrivals
foreach ($newArrivals as $p) {
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

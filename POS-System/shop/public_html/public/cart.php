<?php
$pageTitle = "Your Cart - Victorious General Shop";
include '../includes/header.php';
?>

<!-- 1. PAGE HEADER BAND -->
<section class="bg-surface-gray py-8">
    <div class="container mx-auto px-4">
        <!-- Breadcrumb -->
        <nav class="mb-4">
            <ol class="flex items-center space-x-2 text-sm font-body text-gray-500">
                <li><a href="index.php" class="hover:text-primary-gold">Home</a></li>
                <li>&gt;</li>
                <li class="text-charcoal font-medium">Cart</li>
            </ol>
        </nav>
        
        <!-- Heading -->
        <h1 class="font-heading font-bold text-charcoal text-3xl md:text-4xl">
            Your Cart
        </h1>
    </div>
</section>

<!-- 2. CART CONTENT -->
<section class="bg-surface-gray py-8">
    <div class="container mx-auto px-4">
        <div id="cart-container">
            <!-- Cart will be rendered here by JavaScript -->
            <div class="flex items-center justify-center py-12">
                <div class="animate-pulse text-gray-400">Loading cart...</div>
            </div>
        </div>
    </div>
</section>

<script>
/**
 * Render cart contents
 */
function renderCart() {
    const container = document.getElementById('cart-container');
    const cart = getCart();
    
    if (cart.length === 0) {
        // Empty state
        container.innerHTML = `
            <div class="bg-neutral-white rounded-2xl shadow-sm p-12 text-center max-w-md mx-auto">
                <svg class="w-24 h-24 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
                </svg>
                <h3 class="font-heading font-semibold text-charcoal text-xl mb-2">Your cart is empty</h3>
                <p class="font-body text-gray-500 text-sm mb-6">Looks like you haven't added anything yet.</p>
                <a href="shop.php" class="inline-block bg-gradient-to-br from-primary-gold to-gold-light text-charcoal px-8 py-3 rounded-full font-heading font-semibold transition-transform hover:scale-105">
                    Start Shopping →
                </a>
            </div>
        `;
        return;
    }
    
    // Cart with items
    const total = getCartTotal();
    const count = getCartCount();
    
    let cartItemsHTML = '';
    cart.forEach(item => {
        const lineTotal = item.price * item.qty;
        cartItemsHTML += `
            <div class="flex items-center gap-4 py-4 border-b border-gray-200 last:border-b-0">
                <!-- Product Image -->
                <div class="w-20 h-20 flex-shrink-0">
                    <img src="${item.image}" alt="${item.name}" class="w-full h-full object-cover rounded-lg">
                </div>
                
                <!-- Product Info -->
                <div class="flex-1 min-w-0">
                    <a href="product.php?slug=${item.slug}" class="font-heading font-semibold text-charcoal text-base hover:text-primary-gold transition-colors line-clamp-1">
                        ${item.name}
                    </a>
                    <p class="font-body text-gray-500 text-sm">${item.unit}</p>
                    <p class="font-heading font-semibold text-charcoal text-base mt-1">
                        KSh ${lineTotal.toLocaleString()}
                    </p>
                </div>
                
                <!-- Quantity Controls -->
                <div class="flex items-center space-x-2">
                    <button onclick="updateCartQty('${item.slug}', ${item.qty - 1})" 
                            class="w-8 h-8 border-2 border-gray-300 rounded-lg flex items-center justify-center hover:border-primary-gold transition-colors">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/>
                        </svg>
                    </button>
                    <input type="number" 
                           value="${item.qty}" 
                           min="1" 
                           max="${MAX_QTY}"
                           onchange="updateCartQty('${item.slug}', parseInt(this.value))"
                           class="w-16 h-8 text-center border-2 border-gray-300 rounded-lg focus:outline-none focus:border-primary-gold text-sm">
                    <button onclick="updateCartQty('${item.slug}', ${item.qty + 1})" 
                            class="w-8 h-8 border-2 border-gray-300 rounded-lg flex items-center justify-center hover:border-primary-gold transition-colors">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                        </svg>
                    </button>
                </div>
                
                <!-- Remove Button -->
                <button onclick="removeCartItem('${item.slug}')" 
                        class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-red-500 transition-colors"
                        title="Remove item">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                </button>
            </div>
        `;
    });
    
    container.innerHTML = `
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <!-- LEFT - Cart Items -->
            <div class="lg:col-span-2">
                <div class="bg-neutral-white rounded-2xl shadow-sm p-6">
                    <h2 class="font-heading font-semibold text-charcoal text-xl mb-4">Cart Items</h2>
                    <div class="space-y-0">
                        ${cartItemsHTML}
                    </div>
                </div>
            </div>
            
            <!-- RIGHT - Order Summary -->
            <div class="lg:col-span-1">
                <div class="bg-neutral-white rounded-2xl shadow-sm p-6 lg:sticky lg:top-24">
                    <h2 class="font-heading font-semibold text-charcoal text-lg mb-4">Order Summary</h2>
                    
                    <!-- Subtotal -->
                    <div class="flex justify-between items-center mb-2">
                        <span class="font-body text-charcoal text-sm">Subtotal (${count} item${count !== 1 ? 's' : ''})</span>
                        <span class="font-heading font-semibold text-charcoal text-base">KSh ${total.toLocaleString()}</span>
                    </div>
                    
                    <!-- Delivery Note -->
                    <p class="font-body text-gray-500 text-xs italic mb-4">
                        Delivery fee calculated after order is confirmed via WhatsApp
                    </p>
                    
                    <div class="border-t border-gray-200 my-4"></div>
                    
                    <!-- Total -->
                    <div class="flex justify-between items-center mb-6">
                        <span class="font-heading font-bold text-charcoal text-lg">Total</span>
                        <span class="font-heading font-bold text-primary-gold text-xl">KSh ${total.toLocaleString()}</span>
                    </div>
                    
                    <!-- WhatsApp Button -->
                    <button onclick="sendToWhatsApp()" 
                            class="w-full bg-[#25D366] text-neutral-white py-3 px-6 rounded-lg font-heading font-semibold mb-3 flex items-center justify-center space-x-2 transition-transform hover:scale-105">
                        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413Z"/>
                        </svg>
                        <span>Send Order via WhatsApp</span>
                    </button>
                    
                    <!-- Continue Shopping -->
                    <a href="shop.php" class="block text-center font-body text-primary-gold text-sm hover:underline">
                        Continue Shopping
                    </a>
                </div>
            </div>
        </div>
    `;
}

/**
 * Update cart quantity from cart page
 */
function updateCartQty(slug, newQty) {
    updateQty(slug, newQty);
    renderCart();
}

/**
 * Remove item from cart page
 */
function removeCartItem(slug) {
    if (confirm('Remove this item from cart?')) {
        removeFromCart(slug);
        renderCart();
    }
}

/**
 * Send order to WhatsApp
 */
function sendToWhatsApp() {
    const cart = getCart();
    if (cart.length === 0) {
        alert('Your cart is empty');
        return;
    }
    
    const whatsappUrl = buildWhatsAppMessage(cart);
    console.log('Opening WhatsApp with URL:', whatsappUrl); // Debug log
    
    // Try to open WhatsApp
    const whatsappWindow = window.open(whatsappUrl, '_blank');
    
    // Check if popup was blocked
    if (!whatsappWindow || whatsappWindow.closed || typeof whatsappWindow.closed === 'undefined') {
        // Popup was blocked, show message and try direct navigation
        if (confirm('Please allow popups to send your order via WhatsApp. Click OK to try again.')) {
            window.location.href = whatsappUrl;
            return; // Don't redirect to confirmation if we're navigating away
        }
    }
    
    // Redirect to order confirmation after a short delay
    setTimeout(() => {
        window.location.href = 'order-confirmation.php';
    }, 1000);
}

// Render cart on page load
document.addEventListener('DOMContentLoaded', function() {
    renderCart();
});
</script>

<?php include '../includes/footer.php'; ?>

// Victorious General Shop - Cart System
// Uses browser localStorage for cart persistence

const CART_KEY = 'victorious_cart';
const MAX_QTY = 99;

/**
 * Get cart from localStorage
 * @returns {Array} Cart items array
 */
function getCart() {
    try {
        const cartData = localStorage.getItem(CART_KEY);
        return cartData ? JSON.parse(cartData) : [];
    } catch (error) {
        console.error('Error reading cart:', error);
        return [];
    }
}

/**
 * Save cart to localStorage
 * @param {Array} cart - Cart items array
 */
function saveCart(cart) {
    try {
        localStorage.setItem(CART_KEY, JSON.stringify(cart));
    } catch (error) {
        console.error('Error saving cart:', error);
    }
}

/**
 * Add product to cart
 * @param {Object} product - Product object with slug, name, price, unit, image
 * @param {Number} qty - Quantity to add
 */
function addToCart(product, qty = 1) {
    const cart = getCart();
    const existingItem = cart.find(item => item.slug === product.slug);
    
    if (existingItem) {
        existingItem.qty = Math.min(existingItem.qty + qty, MAX_QTY);
    } else {
        cart.push({
            slug: product.slug,
            name: product.name,
            price: product.price,
            unit: product.unit,
            image: product.image,
            qty: Math.min(qty, MAX_QTY)
        });
    }
    
    saveCart(cart);
    updateCartBadge();
}

/**
 * Remove item from cart
 * @param {String} slug - Product slug
 */
function removeFromCart(slug) {
    let cart = getCart();
    cart = cart.filter(item => item.slug !== slug);
    saveCart(cart);
    updateCartBadge();
}

/**
 * Update item quantity
 * @param {String} slug - Product slug
 * @param {Number} newQty - New quantity
 */
function updateQty(slug, newQty) {
    let cart = getCart();
    
    if (newQty <= 0) {
        cart = cart.filter(item => item.slug !== slug);
    } else {
        const item = cart.find(item => item.slug === slug);
        if (item) {
            item.qty = Math.min(newQty, MAX_QTY);
        }
    }
    
    saveCart(cart);
    updateCartBadge();
}

/**
 * Get cart total
 * @returns {Number} Total price
 */
function getCartTotal() {
    const cart = getCart();
    return cart.reduce((total, item) => total + (item.price * item.qty), 0);
}

/**
 * Get cart item count
 * @returns {Number} Total quantity of all items
 */
function getCartCount() {
    const cart = getCart();
    return cart.reduce((count, item) => count + item.qty, 0);
}

/**
 * Update cart badge in header
 */
function updateCartBadge() {
    const badge = document.getElementById('cart-badge');
    if (badge) {
        const count = getCartCount();
        badge.textContent = count;
        badge.style.display = count > 0 ? 'flex' : 'flex'; // Always show badge
    }
}

/**
 * Show toast notification
 * @param {String} message - Message to display
 */
function showToast(message) {
    // Remove existing toast if any
    const existingToast = document.getElementById('cart-toast');
    if (existingToast) {
        existingToast.remove();
    }
    
    // Create toast element
    const toast = document.createElement('div');
    toast.id = 'cart-toast';
    toast.className = 'fixed bottom-24 left-1/2 transform -translate-x-1/2 bg-neutral-white text-charcoal px-6 py-3 rounded-lg shadow-lg border-l-4 border-primary-gold z-50 transition-opacity duration-300';
    toast.style.opacity = '0';
    toast.innerHTML = `
        <div class="flex items-center space-x-2">
            <svg class="w-5 h-5 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            <span class="font-body font-medium">${message}</span>
        </div>
    `;
    
    document.body.appendChild(toast);
    
    // Fade in
    setTimeout(() => {
        toast.style.opacity = '1';
    }, 10);
    
    // Fade out and remove
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 2000);
}

/**
 * Build WhatsApp message from cart
 * @param {Array} cart - Cart items
 * @returns {String} WhatsApp URL
 */
function buildWhatsAppMessage(cart) {
    let message = "Hi Victorious General Shop, I'd like to order:\n\n";
    
    cart.forEach(item => {
        const lineTotal = item.price * item.qty;
        message += `- ${item.name} (${item.unit}) x${item.qty} - KSh ${lineTotal.toLocaleString()}\n`;
    });
    
    const total = getCartTotal();
    message += `\nTotal: KSh ${total.toLocaleString()}`;
    
    const encodedMessage = encodeURIComponent(message);
    return `https://wa.me/254742071810?text=${encodedMessage}`;
}

/**
 * Clear cart
 */
function clearCart() {
    localStorage.removeItem(CART_KEY);
    updateCartBadge();
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Always update cart badge
    updateCartBadge();
    
    // Wire up Add to Cart buttons
    const addToCartButtons = document.querySelectorAll('.add-to-cart-btn');
    addToCartButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const slug = this.getAttribute('data-product-slug');
            
            // Get product details from global lookup
            if (window.shopProductLookup && window.shopProductLookup[slug]) {
                const product = window.shopProductLookup[slug];
                
                // Get quantity if on product page
                let qty = 1;
                const qtyInput = document.querySelector('input[x-model="quantity"]');
                if (qtyInput) {
                    qty = parseInt(qtyInput.value) || 1;
                }
                
                addToCart(product, qty);
                showToast('Added to cart!');
            } else {
                console.error('Product not found:', slug);
                showToast('Error adding to cart');
            }
        });
    });
    
    // Wire up Buy Now buttons
    const buyNowButtons = document.querySelectorAll('.buy-now-btn');
    buyNowButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const slug = this.getAttribute('data-product-slug');
            
            if (window.shopProductLookup && window.shopProductLookup[slug]) {
                const product = window.shopProductLookup[slug];
                
                let qty = 1;
                const qtyInput = document.querySelector('input[x-model="quantity"]');
                if (qtyInput) {
                    qty = parseInt(qtyInput.value) || 1;
                }
                
                addToCart(product, qty);
                window.location.href = 'cart.php';
            } else {
                console.error('Product not found:', slug);
            }
        });
    });
});

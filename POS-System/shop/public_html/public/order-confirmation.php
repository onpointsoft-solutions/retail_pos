<?php
$pageTitle = "Order Sent - Victorious General Shop";
include '../includes/header.php';
?>

<section class="bg-surface-gray py-16">
    <div class="container mx-auto px-4">
        <div class="max-w-xl mx-auto bg-neutral-white rounded-2xl shadow-md p-8 md:p-12">
            
            <!-- Success Icon -->
            <div class="w-20 h-20 bg-primary-gold bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-6">
                <svg class="w-10 h-10 text-primary-gold" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                </svg>
            </div>
            
            <!-- Heading -->
            <h1 class="font-heading font-bold text-charcoal text-2xl md:text-3xl mb-4 text-center">
                Your Order Has Been Sent! 🎉
            </h1>
            
            <!-- Subtext -->
            <p class="font-body text-charcoal text-base mb-6 text-center">
                We've received your order details on WhatsApp. Our team will confirm availability, delivery cost, and payment details with you shortly.
            </p>
            
            <!-- WhatsApp Info Box -->
            <div class="bg-gold-light bg-opacity-20 rounded-lg p-4 mb-6">
                <p class="font-body text-gray-600 text-sm text-center">
                    📱 Keep an eye on WhatsApp — we typically respond within 30 minutes during business hours (Mon-Sat, 8am-7pm).
                </p>
            </div>
            
            <!-- Divider -->
            <div class="border-t border-gray-200 my-6"></div>
            
            <!-- What Happens Next -->
            <div class="mb-8">
                <h2 class="font-heading font-semibold text-charcoal text-lg mb-4">What happens next?</h2>
                <ol class="space-y-3 font-body text-charcoal text-sm">
                    <li class="flex items-start">
                        <span class="font-semibold text-primary-gold mr-3">1.</span>
                        <span>We confirm your order and item availability</span>
                    </li>
                    <li class="flex items-start">
                        <span class="font-semibold text-primary-gold mr-3">2.</span>
                        <span>We calculate your delivery fee based on your location</span>
                    </li>
                    <li class="flex items-start">
                        <span class="font-semibold text-primary-gold mr-3">3.</span>
                        <span>You confirm payment (M-Pesa or cash on delivery)</span>
                    </li>
                    <li class="flex items-start">
                        <span class="font-semibold text-primary-gold mr-3">4.</span>
                        <span>Your order is packed and delivered to your door</span>
                    </li>
                </ol>
            </div>
            
            <!-- Action Buttons -->
            <div class="flex flex-col sm:flex-row gap-4 mb-6">
                <a href="shop.php" class="flex-1 text-center bg-gradient-to-br from-primary-gold to-gold-light text-charcoal px-6 py-3 rounded-full font-heading font-semibold transition-transform hover:scale-105">
                    Continue Shopping
                </a>
                <a href="index.php" class="flex-1 text-center border-2 border-charcoal text-charcoal px-6 py-3 rounded-full font-heading font-semibold transition-all hover:bg-charcoal hover:text-neutral-white">
                    Back to Home
                </a>
            </div>
            
            <!-- Direct WhatsApp Link -->
            <p class="font-body text-gray-500 text-xs text-center">
                Didn't get a WhatsApp message? 
                <a href="https://wa.me/254742071810?text=Hi%20Victorious%20General%20Shop%2C%20I%27d%20like%20to%20ask%20about%20my%20order" 
                   target="_blank" 
                   class="text-primary-gold hover:underline">Message Us Directly</a>
            </p>
        </div>
    </div>
</section>

<script>
// Clear cart after successful order
document.addEventListener('DOMContentLoaded', function() {
    clearCart();
});
</script>

<?php include '../includes/footer.php'; ?>

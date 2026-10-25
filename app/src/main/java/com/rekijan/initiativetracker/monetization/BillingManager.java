package com.rekijan.initiativetracker.monetization;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchaseState;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.Dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Google Play Billing (v9) for the app: the two tips ("Tip the dev") and Remove Ads. Same
 * structure as the BillingManager in Secret Roller and Mook Roller.
 * <p>
 * Tips are consumable: acknowledged, then consumed so they can be bought again. Remove Ads is a
 * one-time product: acknowledged, never consumed, and restored from Google Play on every start.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String ITEM_SKU_FIVE = "fiveeurodonation";
    private static final String ITEM_SKU_TEN = "teneurodonation";
    private static final String ITEM_SKU_REMOVE_ADS = "remove_ads";
    private static final String LOG_TAG = "BillingManager";

    private final Application app;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private BillingClient billingClient;
    private RemoveAdsListener removeAdsListener;

    /**
     * Notified when the Remove Ads purchase is confirmed (fresh purchase, or found on restore)
     * and when its price has loaded, so the menu can update. Always called on the main thread.
     */
    public interface RemoveAdsListener {
        void onAdsRemoved();

        void onPriceLoaded();
    }

    public BillingManager(Application app) {
        this.app = app;
    }

    public void setRemoveAdsListener(RemoveAdsListener listener) {
        this.removeAdsListener = listener;
    }

    /** Connects to Google Play, then loads the products and restores earlier purchases. */
    public void start() {
        billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                    queryPurchases();
                } else {
                    Log.e(LOG_TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d(LOG_TAG, "Billing service disconnected.");
            }
        });
    }

    public void endConnection() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int code = billingResult.getResponseCode();
        if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) {
                handlePurchase(p);
            }
        } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            // Remove Ads bought earlier (or on another device) but not known here yet, or an
            // earlier tip that was never consumed: restore or finish it
            Log.d(LOG_TAG, "Item already owned, restoring purchases.");
            queryPurchases();
        } else if (code != BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.e(LOG_TAG, "onPurchasesUpdated() failed with result: " + code);
        }
    }

    /**
     * Shows the tip dialog with the prices Google Play returned, in the user's currency. A tip
     * whose product details did not load is left out.
     */
    public void openTipDialog(Activity activity) {
        ProductDetails fiveDetails = productDetailsMap.get(ITEM_SKU_FIVE);
        ProductDetails tenDetails = productDetailsMap.get(ITEM_SKU_TEN);
        if (formattedPrice(fiveDetails) == null && formattedPrice(tenDetails) == null) {
            Toast.makeText(activity, R.string.billing_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_tip, null);
        AlertDialog dialog = Dialogs.builder(activity)
                .setTitle(R.string.tip_dialog_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        bindTip(activity, dialog, content.findViewById(R.id.five_btn),
                content.findViewById(R.id.five_full_price_textView), fiveDetails);
        bindTip(activity, dialog, content.findViewById(R.id.ten_btn),
                content.findViewById(R.id.ten_full_price_textView), tenDetails);

        dialog.show();
    }

    /** Starts the Remove Ads purchase, or says why it can't (already bought, Play not ready). */
    public void purchaseRemoveAds(Activity activity) {
        if (store().isAdsRemoved()) {
            Toast.makeText(activity, R.string.billing_ads_already_removed, Toast.LENGTH_SHORT).show();
            return;
        }
        ProductDetails details = productDetailsMap.get(ITEM_SKU_REMOVE_ADS);
        if (details == null) {
            Toast.makeText(activity, R.string.billing_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        launchBillingFlow(activity, details);
    }

    /** @return The Google Play price of Remove Ads, or null if it has not loaded (yet). */
    @Nullable
    public String getRemoveAdsPrice() {
        return formattedPrice(productDetailsMap.get(ITEM_SKU_REMOVE_ADS));
    }

    private InitiativeTrackerApp store() {
        return (InitiativeTrackerApp) app;
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String sku : new String[]{ITEM_SKU_FIVE, ITEM_SKU_TEN, ITEM_SKU_REMOVE_ADS}) {
            productList.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Map<String, ProductDetails> loaded = new HashMap<>();
                for (ProductDetails pd : result.getProductDetailsList()) {
                    loaded.put(pd.getProductId(), pd);
                }
                // Billing calls back on a background thread; the map is read on the main thread
                mainHandler.post(() -> {
                    productDetailsMap.clear();
                    productDetailsMap.putAll(loaded);
                    if (removeAdsListener != null) removeAdsListener.onPriceLoaded();
                });
            } else {
                Log.e(LOG_TAG, "queryProductDetailsAsync() failed: " + billingResult.getDebugMessage());
            }
        });
    }

    private void queryPurchases() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (Purchase p : purchases) {
                    handlePurchase(p);
                }
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() != PurchaseState.PURCHASED) return;
        List<String> products = purchase.getProducts();
        boolean isRemoveAds = products.contains(ITEM_SKU_REMOVE_ADS);
        if (!isRemoveAds && !products.contains(ITEM_SKU_FIVE) && !products.contains(ITEM_SKU_TEN)) return;

        if (purchase.isAcknowledged()) {
            if (isRemoveAds) {
                onRemoveAdsConfirmed(false);
            } else {
                consumePurchase(purchase.getPurchaseToken());
            }
            return;
        }
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, result -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.e(LOG_TAG, "acknowledgePurchase() failed: " + result.getDebugMessage());
                return;
            }
            if (isRemoveAds) {
                onRemoveAdsConfirmed(true);
            } else {
                consumePurchase(purchase.getPurchaseToken());
                mainHandler.post(() -> Toast.makeText(app, R.string.billing_tip_thanks, Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Saves the Remove Ads flag and tells the listener, once. Safe to call repeatedly (fresh
     * purchase and the startup restore) and from any thread.
     *
     * @param fresh True right after buying, to thank the user.
     */
    private void onRemoveAdsConfirmed(boolean fresh) {
        mainHandler.post(() -> {
            boolean wasAlreadyKnown = store().isAdsRemoved();
            store().setAdsRemoved(true);
            if (fresh) {
                Toast.makeText(app, R.string.billing_ads_removed, Toast.LENGTH_LONG).show();
            }
            if (!wasAlreadyKnown && removeAdsListener != null) {
                removeAdsListener.onAdsRemoved();
            }
        });
    }

    private void consumePurchase(String token) {
        ConsumeParams params = ConsumeParams.newBuilder()
                .setPurchaseToken(token)
                .build();
        billingClient.consumeAsync(params, (result, outToken) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.e(LOG_TAG, "consumeAsync() failed: " + result.getDebugMessage());
            }
        });
    }

    private void launchBillingFlow(Activity activity, ProductDetails productDetails) {
        BillingFlowParams.ProductDetailsParams pdParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build();
        BillingFlowParams flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pdParams))
                .build();
        billingClient.launchBillingFlow(activity, flow);
    }

    /** @return The Google Play price text of a one-time product, or null if it is not available. */
    @Nullable
    private static String formattedPrice(@Nullable ProductDetails details) {
        if (details == null || details.getOneTimePurchaseOfferDetails() == null) return null;
        return details.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    /** Shows one tip with its price and starts the purchase on tap, or hides it without a price. */
    private void bindTip(Activity activity, AlertDialog dialog, View button, TextView priceView,
                         @Nullable ProductDetails details) {
        String price = formattedPrice(details);
        if (price == null) {
            button.setVisibility(View.GONE);
            priceView.setVisibility(View.GONE);
            return;
        }
        priceView.setText(price);
        button.setOnClickListener(v -> {
            dialog.dismiss();
            launchBillingFlow(activity, details);
        });
    }
}

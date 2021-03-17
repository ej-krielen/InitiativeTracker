package com.rekijan.initiativetracker.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.rekijan.initiativetracker.AppExtension;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.ui.fragments.CharacterDetailFragment;
import com.rekijan.initiativetracker.ui.fragments.EditOrderFragment;
import com.rekijan.initiativetracker.ui.fragments.MainActivityFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private static final String ITEM_SKU_FIVE = "fiveeurodonation";
    private static final String ITEM_SKU_TEN = "teneurodonation";

    private static final String LOG_TAG = "LOG_TAG";

    private BillingClient billingClient;

    // sku details are written during billing client setup and used later for purchase
    private SkuDetails fiveSkuDetails;
    private SkuDetails tenSkuDetails;

    @Override
    protected void onDestroy() {
        billingClient.endConnection();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setPopupTheme(R.style.PopupMenuStyle);

        AppExtension app = (AppExtension) this.getApplicationContext();
        app.initializeData(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment_container, MainActivityFragment.newInstance())
                    .commit();

            if (getResources().getBoolean(R.bool.isTablet) && !app.getCharacterAdapter().getList().isEmpty()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0))
                        .commit();
            }
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(app.showBackNavigation());
        }

        setUpBillingClient();
    }

    private void setUpBillingClient(){
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Log.e(LOG_TAG, "BILLING SETUP FINISHED RESPONSE CODE: "+billingResult.getResponseCode() + " : "+billingResult.getDebugMessage());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    // The BillingClient is ready. You can query in app products here.
                    List<String> skuList = new ArrayList<> ();
                    skuList.add(ITEM_SKU_FIVE);
                    skuList.add(ITEM_SKU_TEN);
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    // Process the result.
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                                        for (SkuDetails skuDetails : skuDetailsList) {
                                            String sku = skuDetails.getSku();
                                            // I set the skudetails here when the billing client is setup so that it can be used later in the purchase flow
                                            if (ITEM_SKU_FIVE.equals(sku)) {
                                                fiveSkuDetails = skuDetails;
                                            } else if (ITEM_SKU_TEN.equals(sku)) {
                                                tenSkuDetails = skuDetails;
                                            }
                                        }
                                    }

                                }
                            });

                    // and query what they have already bought
                    queryPurchases();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    // query the list of purchases in case you want to say thanks or give something
    private void queryPurchases(){
        if (billingClient!=null){
            List<Purchase> listPurchase = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
            if (listPurchase!=null){
                for (Purchase purchase: listPurchase){
                    if (purchase.isAcknowledged() && (purchase.getSku().equals(ITEM_SKU_FIVE) || purchase.getSku().equals(ITEM_SKU_TEN))){
                        // they have the purchase so make it available
                        consumePurchase(purchase.getPurchaseToken(), purchase.getDeveloperPayload());
                    }
                }
            }

        } else {
            setUpBillingClient();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //If done in fragment instead, return false

        //If not on tablet and a CharacterDetailFragment is present pop it from the stack before continuing
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        boolean fragmentExists = getSupportFragmentManager().getBackStackEntryCount() > 0;

        switch (item.getItemId()) {
            case R.id.action_settings_next_turn:
                if (!isTablet && fragmentExists) onSupportNavigateUp();
                nextTurn();
            return false;
            case R.id.action_settings_sort:
            case R.id.action_settings_add_character:
                if (!isTablet && fragmentExists) onSupportNavigateUp();
                return false;
            case R.id.action_settings_delete_character:
            case R.id.action_settings_about:
                return false;
            case R.id.action_settings_tip:
                queryPurchases();
                openTipDialog();
                return false;
//          case R.id.action_settings_select_party:
//                //TODO go to select party screen add in later
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void nextTurn() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0));
            transaction.commit();
        }
    }

    public void replaceCharacterDetailFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(position));
            transaction.commit();
        } else {
            transaction.replace(R.id.main_fragment_container, CharacterDetailFragment.newInstance(position));
            transaction.addToBackStack(null);
            transaction.commit();
            //Enable the back button in action bar
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            AppExtension app = (AppExtension) this.getApplicationContext();
            app.setShowBackNavigation(true);
        }
    }

    public void replaceEditOrderFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_fragment_container, EditOrderFragment.newInstance());
        transaction.addToBackStack(null);
        transaction.commit();
        //Enable the back button in action bar
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        AppExtension app = (AppExtension) this.getApplicationContext();
        app.setShowBackNavigation(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //Disable the back button in action bar only if it is the last fragment
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 1);

        AppExtension app = (AppExtension) this.getApplicationContext();
        app.setShowBackNavigation(getSupportFragmentManager().getBackStackEntryCount() > 1);
        getSupportFragmentManager().popBackStack();
        return true;
    }

    public void openTipDialog() {
        //Build a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        //Add custom layout to dialog
        LayoutInflater inflater = LayoutInflater.from(this);
        final View alertDialogView = inflater.inflate(R.layout.tip_dialog, null);
        builder.setTitle(this.getString(R.string.tip_dialog_title));
        //Set button to close and cancel
        builder.setNegativeButton(this.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        //Get views
        TextView fullFivePriceTextView = alertDialogView.findViewById(R.id.five_full_price_textView);
        TextView fullTenPriceTextView = alertDialogView.findViewById(R.id.ten_full_price_textView);
        Button fiveButton = alertDialogView.findViewById(R.id.five_btn);
        Button tenButton = alertDialogView.findViewById(R.id.ten_btn);

        if (!TextUtils.isEmpty(fiveSkuDetails.getPrice())) fullFivePriceTextView.setText(fiveSkuDetails.getPrice());
        if (!TextUtils.isEmpty(tenSkuDetails.getPrice())) fullTenPriceTextView.setText(tenSkuDetails.getPrice());

        final Activity ref = this;
        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        fiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (billingClient != null) {
                    queryPurchases();
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(fiveSkuDetails)
                            .build();
                    billingClient.launchBillingFlow(ref, flowParams);
                    dialog.dismiss();
                }
            }
        });

        tenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queryPurchases();
                if (billingClient != null) {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(tenSkuDetails)
                            .build();
                    billingClient.launchBillingFlow(ref, flowParams);
                    dialog.dismiss();

                }
            }
        });

        //Show the main dialog
        dialog.show();
    }

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {

            try{
                Toast.makeText(MainActivity.this, "Thank you for your tip", Toast.LENGTH_LONG).show();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.

        }
    }

    // put whatever you want in this method - BUT REMEMBER TO ACKNOWLEDGE THE PURCHASE
    private void handlePurchase(Purchase purchase) {

        if ((purchase.getSku().equals(ITEM_SKU_FIVE) || purchase.getSku().equals(ITEM_SKU_TEN)) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }
    }

    // call this if you want to consume a purchased item so that it can be repurchased
    private void consumePurchase(String purchaseToken, String developerPayload){
        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                Log.e(LOG_TAG, "CONSUMED PURCHASE");

            }
        };

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchaseToken)
//                        .setDeveloperPayload(developerPayload)
                        .build();

        billingClient.consumeAsync(consumeParams, listener);
    }

}
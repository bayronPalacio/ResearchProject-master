package com.example.researchproject.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.researchproject.Classes.Catalogue;
import com.example.researchproject.Classes.Product;
import com.example.researchproject.Customer.ProductCustomerActivity;
import com.example.researchproject.Customer.ProductCustomerAdapter;
import com.example.researchproject.MessageFragment;
import com.example.researchproject.R;
import com.example.researchproject.SearchFragment;
import com.example.researchproject.SwipeToDeleteCallback;
import com.example.researchproject.VolleyService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

public class ProductAdminActivity extends BaseAdminActivity implements UpdateProductFragment.OnCardViewClickedListener,
        AddProductFragment.OnCardViewClickedListener {
    private ArrayList<Product> productsList = new ArrayList<>();
    private static final String TAG = "ProductAdminActivity";
    ConstraintLayout constraintLayout;
    RecyclerView recyclerView;
    BottomNavigationView bottomNavigationView;
    FloatingActionButton fab;
    VolleyService request;
    MessageFragment msgFragment;

    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_admin);

        initToolbar(R.id.toolbar);
        initBottomNavigationView(R.id.bottom_navigation, R.id.home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        constraintLayout = findViewById(R.id.layoutProdAdmin);
        msgFragment = MessageFragment.newInstance(R.drawable.logo, "store");

//        url = "https://myprojectstore.000webhostapp.com/product/";
        url = "http://100.25.155.48/product/";
        request = new VolleyService(ProductAdminActivity.this);
        getProducts();

        fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportFragmentManager().beginTransaction().add(R.id.prodDetailFrag, AddProductFragment.newInstance()).addToBackStack(null).commit();
                removeMsg();
            }
        });
    }

    private void getProducts() {
        request.executeGetRequest(url, new VolleyService.VolleyCallback() {
            @Override
            public void getResponse(String response) {
                try {
                    Log.e("response", response);
                    if (response.equals("empty")) {
                        getSupportFragmentManager().beginTransaction()
                                .add(R.id.frameProdAdmin, msgFragment).commit();
                    } else {
                        Gson gson = new Gson();
                        Catalogue catalogue = gson.fromJson(response, Catalogue.class);
                        ProductAdminAdapter myAdapter = new ProductAdminAdapter(catalogue.getProducts());
                        recyclerView = findViewById(R.id.recyclerViewProductsAdmin);
                        recyclerView.setLayoutManager(new LinearLayoutManager(ProductAdminActivity.this));
                        recyclerView.setAdapter(myAdapter);
                        enableSwipeToDeleteAndUndo(myAdapter);
                    }
                    Log.d(TAG, response);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        });
    }

    private void removeMsg(){
        getSupportFragmentManager().beginTransaction().remove(msgFragment).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                Catalogue catalogue = new Catalogue();
                Gson gson = new Gson();
                Bitmap image = null;
                switch (requestCode){
                    case FILE_REQUEST_CODE_ADD:
                    readCSVAddProducts(uri);
                    catalogue.setProducts(productsList);
                    String jsonCatalogue = gson.toJson(catalogue);
                    request.executePostRequest(url, response -> {
                        try {
                            if (response.equals("true")) {
                                Toast.makeText(ProductAdminActivity.this, "Your catalogue has been updated", Toast.LENGTH_SHORT).show();
                                removeMsg();
                                getProducts();
                            }
                            productsList.clear();
                            Log.d(TAG, response);
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                    }, "products", jsonCatalogue);
                    break;
                case FILE_REQUEST_CODE_UPDATE:
                    readCSVUpdateProducts(uri);
                    catalogue.setProducts(productsList);
                    jsonCatalogue = gson.toJson(catalogue);
                    request.executePostRequest(url, response -> {
                        try {
                            if (response.contains("true")) {
                                Toast.makeText(ProductAdminActivity.this, "Your catalogue has been updated", Toast.LENGTH_SHORT).show();
                                getProducts();
                            }
                            productsList.clear();
                            Log.d(TAG, response);
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                    }, "editProductByList", jsonCatalogue);
                    break;
                    case REQUEST_CAMERA:
                        image = (Bitmap) resultData.getExtras().get("data");
                        break;
                    case SELECT_FILE:
                        Uri filePath = resultData.getData();
                        try {
                            image = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
                if (requestCode == REQUEST_CAMERA || requestCode == SELECT_FILE){
                    String url = "http://100.25.155.48/";

                    //Converting Bitmap to string
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    assert image != null;
                    image.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] imageBytes = baos.toByteArray();
                    String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                    //getResults("searchByImage", encodedImage);
                    request.executePostRequest(url, response -> {
                        try {
                            View view = this.getCurrentFocus();
                            if (view != null) {
                                hideKeyboardFrom(getApplicationContext(), view);
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                            MessageFragment msgFragment = (MessageFragment) getSupportFragmentManager().findFragmentByTag("msgFrag");
                            if (response.equals("no results")) {
                                Log.e("entras2", "no results");
                                msgFragment = MessageFragment.newInstance(R.drawable.logo, "nothing");
                                getSupportFragmentManager().beginTransaction().add(R.id.frameProdAdmin, msgFragment, "msgFrag")
                                        .addToBackStack(null).commit();
                            } else {
                                if (msgFragment != null) {
                                    getSupportFragmentManager().beginTransaction().detach(msgFragment).commit();
                                }
                                ProductAdminAdapter myAdapter = new ProductAdminAdapter(catalogue.getProducts());
                                RecyclerView recyclerView = findViewById(R.id.recyclerViewProducts);
                                recyclerView.setLayoutManager(new LinearLayoutManager(ProductAdminActivity.this));
                                recyclerView.setAdapter(myAdapter);
                                enableSwipeToDeleteAndUndo(myAdapter);
                            }
                            Log.d(TAG, response);
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                    }, "searchByImage", encodedImage);
                }
            }
        }
    }

    private void readCSVAddProducts(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));

            String line;
            if ((line = reader.readLine()) != null) {
//                //here csv Line contains the header line and can be used if needed
            }
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                String barcode = row[0];
                String brand = row[1];
                String category = row[2];
                Double costPrice = Double.parseDouble(row[3]);
                String description = row[4];
                int id = Integer.parseInt(row[5]);
                int invLevel = Integer.parseInt(row[6]);
                int invWarnLevel = Integer.parseInt(row[7]);
                String name = row[8];
                Double price = Double.parseDouble(row[9]);
                String urlImg = row[10];

                Product product = new Product(barcode, brand, category, costPrice, description, id, invLevel, invWarnLevel, name, price, urlImg);
                productsList.add(product);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error reading file..." + ex.getMessage());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Error in id parsing..." + ex.getMessage());
        }
    }

    private void readCSVUpdateProducts(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));

            String line;
            if ((line = reader.readLine()) != null) {
//                //here csv Line contains the header line and can be used if needed
            }
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                Boolean availability = Boolean.parseBoolean(row[0]);
                String barcode = row[1];
                Double costPrice = Double.parseDouble(row[2]);
                String description = row[3];
                int id = Integer.parseInt(row[4]);
                int invLevel = Integer.parseInt(row[5]);
                int invWarnLevel = Integer.parseInt(row[6]);
                Double price = Double.parseDouble(row[7]);

                Product product = new Product(availability, barcode, costPrice, description, id, invLevel, invWarnLevel, price);
                productsList.add(product);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error reading file..." + ex.getMessage());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Error in id parsing..." + ex.getMessage());
        }
    }

    private void enableSwipeToDeleteAndUndo(final ProductAdminAdapter myAdapter) {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

                final int position = viewHolder.getAdapterPosition();
                final Product product = myAdapter.getData().get(position);

                myAdapter.removeItem(position);

                Snackbar snackbar = Snackbar.make(constraintLayout, "Product has been removed from the store", Snackbar.LENGTH_LONG)
                        .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    // Dismiss wasn't because of tapping "UNDO"
                                    // so here delete the item from databse
                                    request.executePostRequest(url, new VolleyService.VolleyCallback() {
                                        @Override
                                        public void getResponse(String response) {
                                            try {
                                                if (response.equals("success")) {
                                                    //Toast.makeText(RegistrationActivity.this, "You have successfully registered \n You will now be redirected to LogIn Page.", Toast.LENGTH_LONG).show();
                                                } else {
                                                    //Toast.makeText(RegistrationActivity.this, "Your account already exits.", Toast.LENGTH_LONG).show();
                                                }
                                                Log.d(TAG, response);
                                            } catch (Exception ex) {
                                                Log.e(TAG, ex.getMessage());
                                            }
                                        }
                                    }, "productDelete", String.valueOf(product.getId()));
                                }
                            }
                        })
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                myAdapter.restoreItem(product, position);
                                recyclerView.scrollToPosition(position);
                            }
                        });
                snackbar.setActionTextColor(Color.WHITE);
                snackbar.show();
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onOpen() {
        bottomNavigationView.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
    }

    @Override
    public void onClose() {
        bottomNavigationView.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
        getProducts();
    }

    @Override
    public void onEnter(String word) {
        if (word.isEmpty()) {
            Toast.makeText(ProductAdminActivity.this, "Please enter a word to start searching!", Toast.LENGTH_LONG).show();
        } else {
            getResults("searchProductsByName", word.trim());
        }
    }

    public void getResults(String namePost, String contentPost) {
        request.executePostRequest(url, response -> {
            try {
                View view = this.getCurrentFocus();
                if (view != null) {
                    hideKeyboardFrom(getApplicationContext(), view);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                MessageFragment msgFragment = (MessageFragment) getSupportFragmentManager().findFragmentByTag("msgFrag");
                if (response.equals("no results")) {
                    msgFragment = MessageFragment.newInstance(R.drawable.logo, "nothing");
                    getSupportFragmentManager().beginTransaction().add(R.id.frameProdAdmin, msgFragment, "msgFrag")
                            .addToBackStack(null).commit();
                } else {
                    if (msgFragment != null) {
                        getSupportFragmentManager().beginTransaction().detach(msgFragment).commit();
                    }
                    Gson gson = new Gson();
                    Catalogue catalogue = gson.fromJson(response, Catalogue.class);
                    ProductAdminAdapter myAdapter = new ProductAdminAdapter(catalogue.getProducts());
                    RecyclerView recyclerView = findViewById(R.id.recyclerViewProducts);
                    recyclerView.setLayoutManager(new LinearLayoutManager(ProductAdminActivity.this));
                    recyclerView.setAdapter(myAdapter);
                    enableSwipeToDeleteAndUndo(myAdapter);
                }
                Log.d(TAG, response);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }, namePost, contentPost);
    }
}

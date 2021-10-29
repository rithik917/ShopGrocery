package com.example.shopgrocery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.shopgrocery.Adapters.AdapterCart;
import com.example.shopgrocery.models.ModelCart;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CartActivity extends AppCompatActivity implements PaymentResultListener {

    private ImageButton backBtn;
    private Button placeOrder;
    private RecyclerView cartItemRv;
    private TextView grandTotal, cartIsEmpty, detailCost, detailDelivery, detailTotal;
    private TableLayout tableLayout;

    private String myLatitude, myLongitude, myPhone, shopId, deliveryFee;
Double amt=0.0;
    private FirebaseAuth mAuth;

    private ProgressDialog mProgressDialog;

    private ArrayList<ModelCart> cartProductList;
    private AdapterCart mAdapterCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        Checkout.preload(getApplicationContext());
        shopId = getIntent().getStringExtra("ShopUid");

        backBtn = findViewById(R.id.backBtn);
        cartItemRv = findViewById(R.id.cartItemRV);
        grandTotal = findViewById(R.id.grandTotalTV);
        placeOrder = findViewById(R.id.placeOrder);
        cartIsEmpty = findViewById(R.id.cartIsEmpty);
        detailCost = findViewById(R.id.detailCost);
        detailDelivery = findViewById(R.id.detailDelivery);
        detailTotal = findViewById(R.id.detailTotal);
        tableLayout = findViewById(R.id.tableLayout);

        mAuth = FirebaseAuth.getInstance();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Please wait");
        mProgressDialog.setCanceledOnTouchOutside(false);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        loadCartItems();
        loadMyInfo();

        placeOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myLatitude.equals("") || myLatitude.equals("null") || myLongitude.equals("") || myLongitude.equals("null")){
                    Toast.makeText(CartActivity.this, "Please enter your address in your profile before placing order", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (myPhone.equals("") || myPhone.equals("null")){
                    Toast.makeText(CartActivity.this, "Please enter your phone number in your profile before placing order", Toast.LENGTH_SHORT).show();
                    return;
                }
//
//                Intent intent = new Intent(CartActivity.this, PaymentActivity.class);
//                intent.putExtra("ShopId", shopId);
//                intent.putExtra("Latitude", myLatitude);
//                intent.putExtra("Longitude", myLongitude);
////                int upiTotal = Integer.parseInt(grandTotal.getText().toString())+Integer.valueOf(deliveryFee);
////                Log.d("upiTotal", upiTotal+"");
//                intent.putExtra("GrandTotal", grandTotal.getText().toString().trim());
//                startActivity(intent);
                makepayment();
            }
        });


    }
    private void makepayment()
    {

        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_6BGEx2rzR0KKK1");

        checkout.setImage(R.drawable.grocery_cart);
        final Activity activity = this;

        try {
            JSONObject options = new JSONObject();

            options.put("name", "Shop Grocery");
            options.put("description", "Reference No. #123456");
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png");
            // options.put("order_id", "order_DBJOWzybf0sJbb");//from response of step 3.
            options.put("theme.color", "#3399cc");
            options.put("currency", "INR");
            options.put("amount", "30000");//300 X 100
            options.put("prefill.email", "shopgrocery@gmail.com");
            options.put("prefill.contact","7007814821");
            checkout.open(activity, options);
        } catch(Exception e) {
            Log.e("TAG", "Error in starting Razorpay Checkout", e);
        }
    }
    @Override
    public void onPaymentSuccess(String s)
    {
        submitOrder();

    }
    @Override
    public void onPaymentError(int i, String s) {
        Toast.makeText(CartActivity.this, ""+s, Toast.LENGTH_SHORT).show();
    }
    private void submitOrder() {
        mProgressDialog.setMessage("Placing Order....");
        mProgressDialog.show();

        final String timestamp = ""+System.currentTimeMillis();
        String cost = grandTotal.getText().toString();

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("orderId", timestamp);
        hashMap.put("orderTime", timestamp);
        hashMap.put("orderStatus", "In Progress");
        hashMap.put("orderCost", cost);
        hashMap.put("orderBy", ""+mAuth.getUid());
        hashMap.put("OrderFrom", ""+shopId);
        hashMap.put("latitude", myLatitude);
        hashMap.put("longitude", myLongitude);

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(shopId).child("Orders");
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid());
                        ref1.child("CartItem").child(shopId).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()){
                                    String finalPrice =""+ds.child("finalPrice").getValue();
                                    String productCategory =""+ds.child("prductCategory").getValue();
                                    String ItemImage = ""+ds.child("profileImage").getValue();
                                    String quantity = ""+ds.child("quantity").getValue();
                                    String title = ""+ds.child("title").getValue();
                                    String pId = ""+ds.child("productId").getValue();

                                    HashMap<String, String> hashMap1 = new HashMap<>();
                                    hashMap1.put("finalPrice", finalPrice);
                                    hashMap1.put("productCategory", productCategory);
                                    hashMap1.put("ItemImage", ItemImage);
                                    hashMap1.put("quantity", quantity);
                                    hashMap1.put("title", title);
                                    hashMap1.put("pId", pId);

                                    Log.d("title", title);

                                    ref.child(timestamp).child("items").child(pId).setValue(hashMap1);
                                }
                                mProgressDialog.dismiss();
                                Toast.makeText(CartActivity.this, "Order Placed Successfully...", Toast.LENGTH_SHORT).show();
prepareNotificationMessage(timestamp);

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                        ref1.child("CartItem").removeValue().equals(shopId);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(CartActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(mAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                            String name = ""+ds.child("name").getValue();
                            String email = ""+ds.child("email").getValue();
                            myPhone = ""+ds.child("phone").getValue();
                            String profileImage = ""+ds.child("profileImage").getValue();
                            String accountType = ""+ds.child("accountType").getValue();
                            String city = ""+ds.child("city").getValue();
                            String deliveryFee = ""+ds.child("deliveryFee").getValue();
                            myLatitude = ""+ds.child("latitude").getValue();
                            myLongitude = ""+ds.child("longitude").getValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadCartItems() {

        cartProductList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(mAuth.getUid()).child("CartItem").child(shopId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        cartProductList.clear();
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            ModelCart modelCart = ds.getValue(ModelCart.class);
                            cartProductList.add(modelCart);
                        }
                        mAdapterCart = new AdapterCart(CartActivity.this, cartProductList);
                        cartItemRv.setAdapter(mAdapterCart);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        ref.child(mAuth.getUid()).child("CartItem").child(shopId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    placeOrder.setVisibility(View.VISIBLE);
                    tableLayout.setVisibility(View.VISIBLE);
                    cartIsEmpty.setVisibility(View.GONE);
                    grandTotalPrice((Map<String, Object>) dataSnapshot.getValue());
                } else {
                    grandTotal.setText("Rs 0");
                    cartIsEmpty.setVisibility(View.VISIBLE);
                    placeOrder.setVisibility(View.GONE);
                    tableLayout.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void grandTotalPrice(Map<String, Object> items) {
        ArrayList<String> total = new ArrayList<>();
        Double sum = 0.0;
        final Double[] detailTotals = {0.0};

        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Map singleUser = (Map) entry.getValue();
            total.add((String) singleUser.get("finalPrice"));
        }
        for (int i = 0; i < total.size(); i++) {
            total.set(i, total.get(i).replace("Rs", ""));
            sum += Double.parseDouble(total.get(i));
        }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        final Double finalSum = sum;
        ref.child(shopId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        deliveryFee = ""+dataSnapshot.child("deliveryFee").getValue();
                        Log.d("delivery fee", deliveryFee);
                        detailDelivery.setText("Rs"+deliveryFee);
                        detailTotals[0] = finalSum + Double.parseDouble(deliveryFee);
                        amt=finalSum+ Double.parseDouble(deliveryFee);
                        detailTotal.setText(String.valueOf(detailTotals[0]));
                        grandTotal.setText(String.valueOf(detailTotals[0]));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
        detailCost.setText("Rs"+sum);

    }
    private void prepareNotificationMessage(String orderId){
        String NOTIFICATION_TOPIC="/topics/" +Constants.FCM_TOPIC;
        String NOTIFICATION_TITLE="New order "+orderId;
        String NOTIFICATION_MESSAGE="Congratulations! You have a new order";
        String NOTIFICATION_TYPE="New order";
        JSONObject notificationJo=new JSONObject();
        JSONObject notificationBodyJo=new JSONObject();
        try{
            notificationBodyJo.put("notificationType",NOTIFICATION_TYPE);
            notificationBodyJo.put("buyerUid",mAuth.getUid());
            notificationBodyJo.put("sellerUid",shopId);
            notificationBodyJo.put("notificationTitle",NOTIFICATION_TITLE);
            notificationBodyJo.put("notificationMessage",NOTIFICATION_MESSAGE);
            notificationJo.put("to",NOTIFICATION_TOPIC);
            notificationJo.put("data",notificationBodyJo);

        } catch (JSONException e) {
            Toast.makeText(this,""+e.getMessage(),Toast.LENGTH_SHORT).show();

        }
        sendFcmNotification(notificationJo,orderId);
    }
    private void sendFcmNotification(JSONObject notificationJo,String orderId){
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Intent intent = new Intent(CartActivity.this, OrderDetailsBuyerActivity.class);
                intent.putExtra("orderFrom", shopId);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Intent intent = new Intent(CartActivity.this, OrderDetailsBuyerActivity.class);
                intent.putExtra("orderFrom", shopId);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            }
        }){
            public Map<String,String> getHeaders() throws AuthFailureError {
                Map<String,String> headers=new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Authorization","key=" +Constants.FCM_KEY);
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }
}



package com.example.ft_hangouts;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ft_hangouts.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        
        // Set up FAB based on current destination
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main);
                int currentDestId = navController.getCurrentDestination().getId();
                
                if (currentDestId == R.id.nav_contacts) {
                    // If on contacts page, add new contact
                    navController.navigate(R.id.action_nav_contacts_to_contactFormFragment);
                } else if (currentDestId == R.id.nav_messages) {
                    // If on messages page, create new message
                    navController.navigate(R.id.action_nav_messages_to_contactSelectorFragment);
                } else {
                    // If somewhere else, navigate to contacts first
                    navController.navigate(R.id.nav_contacts);
                    navController.navigate(R.id.action_nav_contacts_to_contactFormFragment);
                }
            }
        });
        
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        
        // Set up the top level destinations
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_contacts, R.id.nav_messages)
                .setOpenableLayout(drawer)
                .build();
                
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        
        // Update FAB icon based on current destination
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_contacts) {
                binding.appBarMain.fab.setImageResource(R.drawable.ic_person_add);
            } else if (destination.getId() == R.id.nav_messages) {
                binding.appBarMain.fab.setImageResource(R.drawable.ic_new_message);
            }
            
            // Hide FAB on detail pages
            if (destination.getId() == R.id.contactFormFragment || 
                destination.getId() == R.id.messageFragment ||
                destination.getId() == R.id.contactSelectorFragment) {
                binding.appBarMain.fab.hide();
            } else {
                binding.appBarMain.fab.show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
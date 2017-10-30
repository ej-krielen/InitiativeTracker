package com.rekijan.initiativetracker.ui.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.rekijan.initiativetracker.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //Done in fragment instead
        switch (item.getItemId()) {
            case R.id.action_settings_next_turn:
                return false;
            case R.id.action_settings_sort:
                return false;
            case R.id.action_settings_add_character:
                return false;
//          case R.id.action_settings_select_party:
//                //TODO go to select party screen add in later
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

package unipd.elia.deltacore.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v13.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v13.FragmentPagerItems;

import unipd.elia.deltacore.R;
import unipd.elia.deltacore.ui.fragments.DownloadExperimentsFragment;
import unipd.elia.deltacore.ui.fragments.ManageExperimentsFragment;

public class TabbedMainActivity extends AppCompatActivity {

    private ViewPager myViewPager;
    private FragmentPagerItemAdapter myPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed_main);

        FragmentPagerItemAdapter adapter = new FragmentPagerItemAdapter(
                getFragmentManager(), FragmentPagerItems.with(this)
                .add("Manage Experiments", ManageExperimentsFragment.class)
                .add("Download Experiments", DownloadExperimentsFragment.class)
                .create());


        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);

        SmartTabLayout viewPagerTab = (SmartTabLayout) findViewById(R.id.viewpagertab);
        viewPagerTab.setViewPager(viewPager);

        myViewPager = viewPager;
        myPagerAdapter = adapter;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tabbed_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        if(id == R.id.action_refresh){
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    public void experimentDownloaded(){
        ((ManageExperimentsFragment) myPagerAdapter.getPage(0)).reloadAndRebindExperiments();
    }

}

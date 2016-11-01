package com.kilogramm.mattermost.view.createChannelGroup;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.databinding.ActivityCreateChannelGroupBinding;
import com.kilogramm.mattermost.presenter.CreateNewChGrPresenter;
import com.kilogramm.mattermost.view.BaseActivity;

import nucleus.factory.RequiresPresenter;

/**
 * Created by melkshake on 31.10.16.
 */

@RequiresPresenter(CreateNewChGrPresenter.class)
public class CreateNewChGrActivity extends BaseActivity<CreateNewChGrPresenter> {
    public final String IS_CHANNEL = "isChannel";

    private ActivityCreateChannelGroupBinding binding;

    private boolean isChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isChannel = getIntent().getBooleanExtra(IS_CHANNEL, false);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_channel_group);
        init();
    }

    private void init() {
        setupToolbar(isChannel ? getString(R.string.create_new_ch_gr_toolber_ch)
                : getString(R.string.create_new_ch_gr_toolber_gr),
                true);
        setColorScheme(R.color.colorPrimary, R.color.colorPrimaryDark);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_channel_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finishActivity();
                break;

            case R.id.action_create:
                if (binding.tvChannelName.getText().length() != 0) {
                    if (isChannel) {
                        getPresenter().requestCreateChannel(binding.tvChannelName.getText().toString(),
                                                            binding.header.getText().toString(),
                                                            binding.purpose.getText().toString());
                        break;
                    } else {
                        getPresenter().requestCreateGroup(binding.tvChannelName.getText().toString(),
                                                          binding.header.getText().toString(),
                                                          binding.purpose.getText().toString());
                    }
                } else {
                    String errorText = isChannel ? "     Channel name is required \n" : "     Group name is required \n";
                    getPresenter().sendShowError(errorText);
                }
                BaseActivity.hideKeyboard(this);
                break;
            default:
                super.onOptionsItemSelected(item);

        }
        return true;
    }

    public void finishActivity() {
        this.finish();
    }
}
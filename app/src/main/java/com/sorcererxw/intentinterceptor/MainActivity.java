package com.sorcererxw.intentinterceptor;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sorcererxw.intentinterceptor.models.DataBean;
import com.sorcererxw.intentinterceptor.ui.adapters.DataAdapter;
import com.sorcererxw.intentinterceptor.utils.DataUtil;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @BindView(R.id.textView_hint)
    TextView mHintTextView;

    @BindView(R.id.editFrom)
    TextView eFrom;

    @BindView(R.id.editTo)
    TextView eTo;

    private DataAdapter mDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        DataUtil.createFile(this);
        mDataAdapter = new DataAdapter(this);
        mRecyclerView.setAdapter(mDataAdapter);
        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        eFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            //输入后调用该方法
            @Override
            public void afterTextChanged(Editable s) {
                tryUpdateList();
            }
        });
        eTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            //输入后调用该方法
            @Override
            public void afterTextChanged(Editable s) {
                tryUpdateList();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryUpdateList();


    }
    private void tryUpdateList(){
        try {
            updateList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void updateList() throws IOException {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    subscriber.onNext(DataUtil.read());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).map(new Func1<String, List<DataBean>>() {
            @Override
            public List<DataBean> call(String s) {
                List<DataBean> list = new ArrayList<>();
                Gson gson = new Gson();
                JsonParser parser = new JsonParser();
                JsonElement el = parser.parse(s);
                JsonArray jsonArray = el.getAsJsonArray();
                for (JsonElement je : jsonArray) {
                    DataBean dataBean = gson.fromJson(je, DataBean.class);
                    if (dataBean != null) {
                        list.add(dataBean);
                    }
                }
                Collections.reverse(list);
                return list;
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<DataBean>>() {
                    @Override
                    public void call(List<DataBean> dataBeen) {
                        List<DataBean> dataBeenDisp = dataBeen;
                        if (dataBeen.size() == 0) {
                            mHintTextView.setText(getString(R.string.hint_not_data).replace("|","\n"));
                        } else {
                            mHintTextView.setText("");
                            String sf = eFrom.getText().toString(), st = eTo.getText().toString();
                            if (!(TextUtils.isEmpty(sf)&&TextUtils.isEmpty(st))) {
                                dataBeenDisp = new ArrayList<>();
                                for (DataBean d : dataBeen) {
                                    if ((!TextUtils.isEmpty(sf) && d.getFrom().contains(sf)) || (!TextUtils.isEmpty(st) && d.getTo().contains(st))) {
                                        dataBeenDisp.add(d);
                                    }
                                }
                            }
                        }
                        mDataAdapter.setData(dataBeenDisp);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                try {
                    DataUtil.clear();
                    mDataAdapter.clearData();
                    mHintTextView.setText(getString(R.string.hint_not_data).replace("|","\n"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.action_save:
                 DataUtil.writeAllData(mDataAdapter.getData());
                return true;
            case R.id.action_share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, mDataAdapter.getData().toString());
                //shareIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

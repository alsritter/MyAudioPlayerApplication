package com.alsritter.myaudioplayerapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.trendyol.bubblescrollbarlib.BubbleScrollBar;
import com.trendyol.bubblescrollbarlib.BubbleTextProvider;

import java.io.IOException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView nextIv, playIv, lastIv, shuffleIv, replayIv;
    SeekBar progress_bar;
    TextView singerTv, songTv, total_timeTv, now_timeTv;
    RecyclerView musicRv;
    BubbleScrollBar bubbleScrollBar;
    //创建一个全局的LocalMusicBean变量
    LocalMusicBean tempMusicBean1;

    //数据源
    private List<LocalMusicBean> mData;
    //用来储存临时的数据
    private List<LocalMusicBean> tempData;

    private LocalMusicAdapter adapter;

    //记录当前正在播放的音乐的位置（默认是-1，即没有音乐） tempCurrentPlayPosition是用来检查当前是否改变了
    private int currentPlayPosition = -1;
    private int tempCurrentPlayPosition = -1;
    //添加一个布尔变量用来记录是否有音乐在播放
    private boolean isChangeMusic = false;

    //用来判断是否正在拖动进度条，来防止更新进度条冲突
    private boolean isDrag = false;

    //    记录进度条的位置
    private int currentPosition = 0;

    //创建一个媒体播放器
    private MediaPlayer mediaPlayer;

    //播放模式：0是顺序，1是乱序
    private PlayMode playMode = PlayMode.LIST;

    //播放过的列表
    private List<LocalMusicBean> playlist;

    /*在挂断电话的时候，用于判断是否为是来电时中断*/
    private boolean isCellPlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //因为从安卓6开始(即sdk23)就不能只在AndroidMagnify申请权限,要动态的申请,不然会直接崩溃
        //这里是检查权限许可： ContextCompat：上下文兼容性      checkSelfPermission：检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //如果没有PackageManager.PERMISSION_GRANTED（即：权限许可）则申请权限
            //注意，当执行了这个 requestPermissions方法之后会自动回调下面那个onRequestPermissionsResult方法
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            //只有允许的权限才可以进行下一步操作
            //初始化操作
            initView();
            //初始化各种回调函数
            initData();
            //初始化音乐排序
            startSort();
        }
    }


    //    创建一个播放模式通用类当作枚举
    enum PlayMode {
        RANDOM, LIST
    }

    //    写一个排序，只调用一次，以后直接替换数据就好了
    private void startSort() {
//                按名字顺序排序
//                思路创建多个list分别装数据，最后把它们拼接到一起
        List<LocalMusicBean> english = new ArrayList<>();
        List<LocalMusicBean> chinese = new ArrayList<>();
        List<LocalMusicBean> number = new ArrayList<>();
        List<LocalMusicBean> other = new ArrayList<>();


        for (LocalMusicBean item : mData) {
            if (item.getSong().substring(0, 1).matches("^[A-Za-z]+$")) {
                english.add(item);
                continue;
            }
            //排列中文字符
            if (item.getSong().substring(0, 1).matches("^[\\u4e00-\\u9fa5]*$")) {
                chinese.add(item);
                continue;
            }
            //再排数字
            if (item.getSong().substring(0, 1).matches("^[0-9]*$")) {
                number.add(item);
            } else {
                //其他的项目
                other.add(item);
            }
        }

        Collections.sort(english, new Comparator<LocalMusicBean>() {
            @Override
            public int compare(LocalMusicBean o1, LocalMusicBean o2) {
                return Collator.getInstance(java.util.Locale.ENGLISH).compare(o1.getSong(), o2.getSong());
            }
        });

        Collections.sort(chinese, new Comparator<LocalMusicBean>() {
            @Override
            public int compare(LocalMusicBean o1, LocalMusicBean o2) {
                return Collator.getInstance(java.util.Locale.CHINA).compare(o1.getSong(), o2.getSong());
            }
        });

        Collections.sort(number, new Comparator<LocalMusicBean>() {
            @Override
            public int compare(LocalMusicBean o1, LocalMusicBean o2) {
                return Collator.getInstance().compare(o1.getSong(), o2.getSong());
            }
        });

        Collections.sort(other, new Comparator<LocalMusicBean>() {
            @Override
            public int compare(LocalMusicBean o1, LocalMusicBean o2) {
                return Collator.getInstance().compare(o1.getSong(), o2.getSong());
            }
        });

        //别忘了先清空mData
        mData.clear();

        mData.addAll(english);
        mData.addAll(chinese);
        mData.addAll(number);
        mData.addAll(other);


        //!!!!!!!注意这里要使用深度拷贝,如果直接 = 的话是拷贝内存地址
        tempData.addAll(mData);
    }


    //    根据模式自动切换list的排序模式
    private void sortMusicList() {
//        先把模式切换
        if (playMode == PlayMode.LIST) {
            playMode = PlayMode.RANDOM;
            shuffleIv.setImageResource(R.drawable.ic_shuffle_black_24dp);
        } else {
            playMode = PlayMode.LIST;
            shuffleIv.setImageResource(R.drawable.ic_repeat_black_24dp);
        }

//        根据当前模式执行特定的排序
        switch (playMode) {
            case LIST:
                //这里同样需要深度拷贝
                mData.clear();
                mData.addAll(tempData);
                //数据源变化，提示适配器更新
                adapter.notifyDataSetChanged();
                break;
            case RANDOM:
//                随机排序
                Collections.shuffle(mData);
                adapter.notifyDataSetChanged();
                break;
        }
    }


    //    更新进度条总时长
    private void updateProgress() {
        if (progress_bar != null && mediaPlayer != null) {
            //先归零
            progress_bar.setProgress(0);
            progress_bar.setMax(mediaPlayer.getDuration());
        }
    }

    //播放指定位置的音乐
    private void playMusicInPosition(LocalMusicBean musicBean) {
        //设置标题的歌名和歌手名
        singerTv.setText(musicBean.getSinger());
        songTv.setText(musicBean.getSong());
        //设置进度条的总时间
        total_timeTv.setText(musicBean.getDuration());
        //先停止音乐，如果已经暂停了就不需要再暂停
        if (mediaPlayer.isPlaying()) {
            stopMusic();
        }
        isChangeMusic = true;
        //重置多媒体播放器
        mediaPlayer.reset();
        //设置新的播放路径
        try {
            mediaPlayer.setDataSource(musicBean.getPath());
            Log.d("TAG", "playMusic: " + musicBean.getPath());
            //重置好之后开始播放音乐
            playMusic();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //播放音乐
    private void playMusic() {
        isDrag = false;
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            //使用一个bool判断当前是处于切歌状态还是播放指定位置的状态
            if (isChangeMusic) {
                //同步播放器以准备播放
                //开始
                try {
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isChangeMusic = false;
            } else {
//                暂停恢复
                mediaPlayer.seekTo(currentPosition);
                mediaPlayer.start();
            }
            progressBarStart();
            playIv.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
        }
    }

    //    暂停播放音乐
    private void pauseMusic() {
        //暂停已停止的行为是非法的MediaPlayer
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            currentPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            playIv.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
        }
    }

    //      停止播放音乐
    private void stopMusic() {
        if (mediaPlayer != null) {
//            先把这个currentPausePosition归零，表示现在不是暂停
            currentPosition = 0;
            mediaPlayer.pause();
            //转到特定的时间位置上
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            //按下停止播放按钮图标改变
            playIv.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
        }
    }

    //      重播音乐
    private void replayMusic() {
        if (tempMusicBean1 != null) {
            playMusicInPosition(tempMusicBean1);
        }
    }


    //    记录播放列表
    private void addPlayItems() {
        if (playlist != null) {
            if (tempCurrentPlayPosition != currentPlayPosition) {
                tempCurrentPlayPosition = currentPlayPosition;
                //            把当前音乐的位置添加到list上方便上一首音乐
                try {
                    //这里要采用深度克隆
                    playlist.add((LocalMusicBean) mData.get(currentPlayPosition).clone());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //    播放完毕自动切换成下一首
    private void switchTheMusic() {
        addPlayItems();
        //因为有两种模式，所以需要先判断目前处于哪种模式
        nextMusic();
    }


    //      上一首音乐
    private void lastMusic() {
//        不管是哪种模式上一首都是按照列表的来
        if (playlist.size() < 1) {
//                    如果当前位置为0表示上面已经没有音乐了
            Toast.makeText(this, "已经是第一首了", Toast.LENGTH_SHORT).show();
            return;
        }
        int temp;
        if (playlist.size() == 1) {
            temp = playlist.size() - 1;
            //深拷贝
            try {
                tempMusicBean1 = (LocalMusicBean) playlist.get(temp).clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "已经是第一首了", Toast.LENGTH_SHORT).show();
        } else {
            temp = playlist.size() - 2;
            try {
                tempMusicBean1 = (LocalMusicBean) playlist.get(temp).clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            playlist.remove(playlist.size() - 1);
        }
        Log.d("List", temp + "******");

//        用完后再删掉
        playMusicInPosition(tempMusicBean1);
        updateProgress();
    }

    //v1

    //    下一首音乐
    private void nextMusic() {
//        先判断playList列表前面是否还有，没有的话再下一首，如果有的话则先读取playList
        Log.d("Mode", "nextMusic: 0");
        if (currentPlayPosition == mData.size() - 1) {
//                    如果当前位置为0表示后面已经没有音乐了
            Toast.makeText(this, "已经是最后一首了", Toast.LENGTH_SHORT).show();
            return;
        }
        currentPlayPosition++;
        addPlayItems();
        try {
            tempMusicBean1 = (LocalMusicBean) mData.get(currentPlayPosition).clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        playMusicInPosition(tempMusicBean1);
        updateProgress();
    }

    //    跳转到歌名
    private void skipMusicName() {
        //        设置RecyclerView的scrollToPosition(滚动位置)
//                注意这里还要考虑切换成乱序模式的情况(所以取最后一个)
        int temp = currentPlayPosition;
        Objects.requireNonNull(musicRv.getLayoutManager()).scrollToPosition(temp == -1 ? 0 : temp);
    }

    /*来电事件处理*/
    private class myPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING://来电，应当停止音乐
                    if (mediaPlayer.isPlaying()) {
                        isCellPlay = true;//标记这是属于来电时暂停的标记
                        pauseMusic();
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE://无电话状态
                    if (isCellPlay) {
                        isCellPlay = false;
                        playMusic();
                    }
                    break;
            }
        }

    }

    //使进度条开始播放
    private void progressBarStart() {
        final SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", java.util.Locale.getDefault());
//                开始让进度条动起来
        //监听播放时间回调函数
        Timer timer = new Timer();
        //创建一个计时器任务
        timer.schedule(new TimerTask() {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (now_timeTv != null && mediaPlayer != null) {
                        now_timeTv.setText(sdf.format(new Date(mediaPlayer.getCurrentPosition())));
                    }
                }
            };

            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    if (!isDrag) {
                        progress_bar.setProgress(mediaPlayer.getCurrentPosition());
                    }
                    runOnUiThread(runnable);
                }
            }

        }, 0, 100);
    }

    //    进程结束时销毁
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //当执行这个销毁方法时不再执行播放
        stopMusic();
    }


    // 前面的获取权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //用法与之前学习的startActivityForResult那个状态码的原理相同
        if (requestCode == 1) {
            // 如果请求结果为空，则表明没有得到权限
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //得到权限
                initData();
                Log.d("TAG", "onRequestPermissionsResult: 取得了权限");
            } else {
                //没有得到权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    //初始化控件
    private void initView() {
        nextIv = findViewById(R.id.the_next_music);
        playIv = findViewById(R.id.the_play_music);
        lastIv = findViewById(R.id.the_last_music);
        singerTv = findViewById(R.id.singer_name);
        songTv = findViewById(R.id.music_name);
        musicRv = findViewById(R.id.music_items);
        shuffleIv = findViewById(R.id.the_shuffle_music);
        replayIv = findViewById(R.id.the_replay_music);
        progress_bar = findViewById(R.id.music_progress_bar);
        total_timeTv = findViewById(R.id.total_time);
        now_timeTv = findViewById(R.id.now_progress);

        nextIv.setOnClickListener(this);
        playIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
        shuffleIv.setOnClickListener(this);
        replayIv.setOnClickListener(this);
        songTv.setOnClickListener(this);
    }

    //初始化各种回调函数
    private void initData() {

        //在这里初始化进度条
        progress_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                   当拖动条的滑块位置发生改变时触发该方法,在这里直接使用参数progress，即当前滑块代表的进度值
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (currentPlayPosition == -1) {
                    Toast.makeText(MainActivity.this, "自动选择第一首", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("------------", "开始滑动！");
                Log.d("------------", String.valueOf(currentPosition));
                isDrag = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("------------", "停止滑动！");
//                    更新进度条的位置
                currentPosition = seekBar.getProgress();
//                    使音乐暂停
                mediaPlayer.pause();
                playMusic();
//                    显示最新的位置
                Log.d("------------", String.valueOf(mediaPlayer.getCurrentPosition()));
                isDrag = false;
            }
        });
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("tag", "播放完毕");
                //根据需要添加自己的代码。。。
                switchTheMusic();
            }
        });

//        初始化list
        mData = new ArrayList<>();
        playlist = new ArrayList<>();
        tempData = new ArrayList<>();


        //创建适配器(同时实现回调函数)
        adapter = new LocalMusicAdapter(this, mData, new LocalMusicAdapter.MyItemOnClickCallback() {
            @Override
            public void onClick(View v, int pos) {
                currentPlayPosition = pos;
                try {
                    tempMusicBean1 = (LocalMusicBean) mData.get(pos).clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                playMusicInPosition(tempMusicBean1);
//                    更新进度条所在时间
                updateProgress();
                addPlayItems();
            }
        });

        //创建一个布局管理器(第三个参数是设置是否反转)
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        musicRv.setLayoutManager(layoutManager);
        musicRv.setAdapter(adapter);


        //bubbleScrollBar.bubbleTextProvider = BubbleTextProvider { sampleAdapter.data[it] }


        //加载本地数据源
        loadLocalMusic();

        //滑动条
        bubbleScrollBar = findViewById(R.id.bubbleScrollBar);
        bubbleScrollBar.attachToRecyclerView(musicRv);
        bubbleScrollBar.setBubbleTextProvider(new BubbleTextProvider() {
            @Override
            public String provideBubbleText(int i) {
                //return mData.get(i).getSong().substring(0,1);
                return new StringBuilder(mData.get(i).getSong().substring(0,1)).toString();
            }
        });

        //监听来电事件
        TelephonyManager phoneMada = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Objects.requireNonNull(phoneMada).listen(new myPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }

    //按钮点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.the_next_music:
                nextMusic();
                break;
            case R.id.the_play_music:
//                要先判断是否选中了音乐，默认是null
                if (currentPlayPosition == -1) {
                    Toast.makeText(this, "请选择要播放的音乐", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mediaPlayer.isPlaying()) {
//                    如果在播放状态则要先暂停
                    pauseMusic();

                } else {
//                    此时没有播放，点击开始音乐
                    playMusic();
                }
                break;
            case R.id.the_last_music:
                lastMusic();
                break;
            case R.id.the_shuffle_music:
                //切换播放模式
                sortMusicList();
                break;
            case R.id.the_replay_music:
                replayMusic();
                break;
            case R.id.music_name:
//                点击歌名自动跳转到指定位置上
                skipMusicName();
                break;
        }
    }


    //  加载本地存储的音乐MP3文件到集合中
    private void loadLocalMusic() {
        //1.获取ContentResolver对象
        ContentResolver resolver = getContentResolver();
        //2.获取本地音乐存储的url地址(EXTERNAL_CONTENT_URI是外部地址)
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //3.开始查询地址(后面的是查询条件)
        Cursor cursor = resolver.query(uri, null, null, null, null);

        //4.遍历cursor
        int id = 0;
        //自定义一个转换器
        //这个转换时间的方法记得加上 java.util.Locale.getDefault()，虽然没事什么卵用，但是为了防止报错
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", java.util.Locale.getDefault());
        String song;
        String singer;
        String album;
        String sid;
        String path;
        String time;
        LocalMusicBean musicBean;
        long duration;


        //这里直接使用cursor.moveToNext()就可以了，但是保险起见加上requireNonNull用来防止该值为空
        while (Objects.requireNonNull(cursor).moveToNext()) {

            song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            id++;
            sid = String.valueOf(id);
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            time = sdf.format(new Date(duration));
            musicBean = new LocalMusicBean(sid, song, singer, album, time, path);
            mData.add(musicBean);
        }
    }


}

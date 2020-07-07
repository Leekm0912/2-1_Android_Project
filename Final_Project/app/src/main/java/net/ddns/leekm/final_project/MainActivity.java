package net.ddns.leekm.final_project;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    XmlPullParser parser; // 파서
    ArrayList<MyItem> arrayList; // 파싱해온 값을 저장해줄 리스트
    EditText editText; // 네이버 아이디를 입력받을 공간
    String xml; // xml의 url
    MyAdapter myAdapter; // 어댑터
    ImageView imageView; // 이미지
    TextView title; // 제목
    TextView desc; // 자기소개
    static HashMap<String, Bitmap> bitmapHash = new HashMap<String, Bitmap>(); // 웹에서 불러온 이미지가 들어갈 해쉬맵

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arrayList = new ArrayList<>();
        editText = findViewById(R.id.editText);
        title = findViewById(R.id.title);
        desc = findViewById(R.id.desc);
        imageView = findViewById(R.id.image);

        // XML을 다운받아주는 메소드
        downloadXML("https://rss.blog.naver.com/skatjrdndqo.xml");
        System.out.println(xml);
        startParse();

        // ListView 작업
        ListView listView = findViewById(R.id.list);
        myAdapter = new MyAdapter(this,R.layout.listview_layout,arrayList);
        listView.setAdapter(myAdapter);

        listView.setOnItemClickListener((parent, view, position, l_position)->{
            // 암시적 호출하기
            Intent intent_naver = new Intent(Intent.ACTION_VIEW, Uri.parse(arrayList.get(position).getUrl()));
            startActivity(intent_naver);
        });
    }


    // XML을 다운받아주는 메서드
    public void downloadXML(String url){
        try {
            DownloadXML downloadXML = new DownloadXML(url);
            downloadXML.start();
            downloadXML.join(); // 작업을 기다려줌
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    // 파싱해주는 메서드
    public void startParse(){
        try {
            // 다운받은 xml을 inputStream으로 전환
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
            XmlPullParserFactory parserFactory = null;
            parserFactory = XmlPullParserFactory.newInstance();
            parser = parserFactory.newPullParser() ;
            parser.setInput(inputStream,"utf-8");
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        try {
            //파싱 시작
            // 일단 이미지를 기본값으로 바꿔줌(블로그에 이미지가 없을때를 대비)
            imageView.setImageResource(R.mipmap.ic_launcher);
            boolean isItemTag = false; // <item> .영역에 인지 여부 체크
            boolean isImageTag = false; // 사진의 url을 구분하기 위함.
            int eventType = parser.getEventType() ;
            String name = "";
            String prev = "";
            String date = "";
            String url = "";
            String tagName = "default";
            String titleText = "";
            String descText = "";
            String imageUrl = "";
            int titleChangeCount = 0; // 왜인지 내용 \t\t\t 내용 형식으로
            int descChangeCount = 0; //  같은 내용이 여러번 파싱이 되어서
            int imgChangeCount = 0; //   처음 1번만 결과에 반영하기 위한 변수들

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    tagName = parser.getName();
                    if(tagName.equals("item")) {
                        isItemTag = true;
                    }else if(tagName.equals("image")){
                        isImageTag = true;
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    String text = parser.getText();
                    if (tagName.equals("title") && isItemTag) {
                        name += text;
                    } else if (tagName.equals("link") && isItemTag) {
                        url += text ;
                    }else if (tagName.equals("description") && isItemTag) {
                        prev += text ;
                    }else if (tagName.equals("pubDate") && isItemTag) {
                        date += text;
                    }
                    if (tagName.equals("title") && !isItemTag) {
                        titleText += text;
                        if(titleChangeCount < 1) { // 처음 1회만 결과에 반영
                            title.setText(titleText);
                            titleChangeCount++;
                        }
                    }else if (tagName.equals("description") && !isItemTag) {
                        descText += text;
                        if(descChangeCount < 1) { // 처음 1회만 결과에 반영
                            desc.setText(descText);
                            descChangeCount++;
                        }
                    }else if (tagName.equals("url") && isImageTag) {
                        imageUrl += text;
                        if(imgChangeCount < 1) { // 처음 1회만 결과에 반영
                            // 이미지를 이미지뷰에 불러오기
                            ImageLoadTask task = new ImageLoadTask(imageUrl,imageView);
                            task.execute();
                            imgChangeCount++;
                        }
                        isImageTag = false;
                    }
                }else if (eventType == XmlPullParser.END_TAG) {
                    String endTag = parser.getName() ;
                    if(endTag.equals("item")){
                        // item 태그가 끝났다면 파싱한 내용을 리스트에 저장한 후
                        MyItem myItem = new MyItem(name, prev, date, url);
                        arrayList.add(myItem);
                        // 초기화
                        name = "";
                        prev = "";
                        date = "";
                        url = "";

                        isItemTag = false;
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }


    public void mClick(View v){
        // 네이버 rss서비스는 https://rss.blog.naver.com/ + 네이버아이디 + .xml 형식으로 되어있음
        String url ="https://rss.blog.naver.com/"+editText.getText()+".xml";
        arrayList = new ArrayList<>();
        // xml을 다운받고
        downloadXML(url);
        // 파싱해서
        startParse();
        // 바뀐 리스트 목록을 새로고침
        myAdapter.notifyDataSetChanged();
        ListView listView = findViewById(R.id.list);
        myAdapter = new MyAdapter(this,R.layout.listview_layout,arrayList);
        listView.setAdapter(myAdapter);

        listView.setOnItemClickListener((parent, view, position, l_position)->{
            // 암시적 호출하기
            Intent intent_naver = new Intent(Intent.ACTION_VIEW, Uri.parse(arrayList.get(position).getUrl()));
            startActivity(intent_naver);
        });

    }


    // XML파일을 다운로드해주는 이너클래스
    class DownloadXML extends Thread{
        String url;

        // 다운로드할 xml의 url 주소
        public DownloadXML(String url) {
            this.url = url;
        }

        @Override
        public void run(){
            try {
                System.out.println(Thread.currentThread().getName());
                URL download_url = new URL(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        download_url.openStream(), "UTF-8"));
                xml = in.readLine();
                while(true){
                    String temp;
                    temp = in.readLine();
                    if(temp==null)
                        break;
                    xml += temp; // 한줄씩 읽어와서 더해줌
                }
            } catch (Exception e) {

            }
        }
    }

    // 웹에서 이미지를 불러오는 이너 클래스
    public class ImageLoadTask extends AsyncTask<Void,Void, Bitmap> {
        private String urlStr;
        private ImageView imageView;

        // 매개변수 : 이미지url, 불러온 이미지를 표시할 이미지뷰.
        public ImageLoadTask(String urlStr, ImageView imageView) {
            this.urlStr = urlStr;
            this.imageView = imageView;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bitmap = null;
            try {
                if (bitmapHash.containsKey(urlStr)) { // 이미 다운받았던 이미지가 있다면(url이 키) 삭제
                    Bitmap oldbitmap = bitmapHash.remove(urlStr);
                    if(oldbitmap != null) {
                        oldbitmap.recycle();
                        oldbitmap = null;
                    }
                }
                URL url = new URL(urlStr);
                // 이미지를 불러옴
                bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                bitmapHash.put(urlStr,bitmap); // 불러온 이미지를 put해줌

            } catch (Exception e) {
                e.printStackTrace();
            }
            // 불러온 이미지를 리턴값으로 전달해서 onPostExecute()(View 스레드)에서 이미지 변경시켜줌
            return bitmap;
        }
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            imageView.setImageBitmap(bitmap); // 불러온 이미지를 이미지뷰에 적용
            imageView.invalidate(); // 화면 갱신(onDraw 호출)
        }
    }


    class MyAdapter extends BaseAdapter{
        Context context;
        LayoutInflater inflater;
        ArrayList<MyItem> list;
        int layout;

        @SuppressLint("ServiceCast")
        public MyAdapter(Context context, int layout, ArrayList<MyItem> item){
            this.context = context;
            this.layout = layout;
            this.list = item;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) { return position; }

        /*
        * position : 생성할 항목의 순서값
        * parent : 생성되는 뷰의 부모(지금은 리스트뷰)
        * convertView : 이전에 생성되었던 차일드 뷰(지금은 Layout.xml) 첫 호출시에는 null
        */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null){
                convertView = inflater.inflate(layout,parent,false);
            }
            TextView t1 = convertView.findViewById(R.id.text1);
            TextView t2 = convertView.findViewById(R.id.text2);
            TextView t3 = convertView.findViewById(R.id.text3);
            t1.setText(list.get(position).getName());
            t2.setText(list.get(position).getPrev());
            t3.setText(list.get(position).getDate());

            return convertView;
        }
    }


    class MyItem{
        private String name;
        private String prev;
        private String date;
        private String url;

        public MyItem(String name, String prev, String date, String url) {
            this.name = name;
            this.prev = prev;
            this.date = date;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getPrev() {
            return prev;
        }

        public String getDate() {
            return date;
        }

        public String getUrl() { return url; }
    }
}
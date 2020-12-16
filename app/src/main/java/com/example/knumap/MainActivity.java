package com.example.knumap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private GoogleMap mMap; // 지도 객체
    MarkerOptions markerOptions;
    Marker marker;
    boolean isUp = false;   // 정보 창 띄움 여부
    DataBaseHelper DBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태 표시줄 숨기기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // 구글 지도
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        DBHelper = new DataBaseHelper(getApplicationContext()); // 데이터 베이스 연동
        try {
            DBHelper.createDataBase(); // 데이터베이스 복사
            DBHelper.openDataBase(); // 데이터베이스 sql문이 사용 가능하도록 변경
            DBHelper.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setMenu();
        search();
    }

    public void setMenu() {
        final Button buttonMenu = (Button) findViewById(R.id.menu);
        buttonMenu.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Drawer 레이아웃
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);

                if (!drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    public void setInfo(int start, int end) {
        LinearLayout info = (LinearLayout) findViewById(R.id.info);
        for (int i = end; i >= start; i--) {
            if (i == 0) continue;
            Button floor = new Button(this);
            floor.clearComposingText();
            if (Build.VERSION.SDK_INT >= 21)
                floor.setStateListAnimator(null);
            floor.setBackgroundResource(R.drawable.button_click);
            floor.setTextAppearance(this, R.style.FloorButton);
            if (i > 0)
                floor.setText(i + "층");
            else
                floor.setText("지하 " + i * (-1) + "층");
            info.addView(floor);
        }
    }

    public void search() {
        final Button buttonCategory = (Button) findViewById(R.id.category);
        final Button buttonSearch = (Button) findViewById(R.id.search_button);

        final Spinner college = (Spinner) findViewById(R.id.college); // 단과대 스피너 생성
        final Spinner department = (Spinner) findViewById(R.id.department);

        final SQLiteDatabase database = DBHelper.getReadableDatabase(); // 데이터베이스 읽기 형식으로 불러오기


        ArrayAdapter<CharSequence> adapterCollege = ArrayAdapter.createFromResource(this, R.array.college, android.R.layout.simple_spinner_item); // 스피터에 넣을 배열 생성
        adapterCollege.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        college.setAdapter(adapterCollege); // 스피너에 적용

        college.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<String> departmentList = new ArrayList<>(); // 학과 스피너에 넣을 리스트 생성

                // sqlite로 부터 단과 대학에 따른 학과 리스트 가져옮
                String sql = "select department from department where college=\""
                        + college.getSelectedItem().toString().replaceAll(" ", "").trim() + "\"";
                Cursor cursor = database.rawQuery(sql, null);
                while (cursor.moveToNext())
                    departmentList.add(cursor.getString(0));

                cursor.close();

                ArrayAdapter<String> departmentAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, departmentList);
                department.setAdapter(departmentAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        buttonCategory.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((Button) v).getText().equals("시설 검색")) { // 버튼 선택시 건물을 검색할 건지 교수님을 검색할 건지 선택
                    ((Button) v).setText("교수 검색");
                    college.setVisibility(View.VISIBLE); // 단과대 스피너 드러나게 함
                    department.setVisibility(View.VISIBLE); // 학과 스피너 드러나게 함
                } else {
                    ((Button) v).setText("시설 검색");
                    college.setVisibility(View.GONE); // 단과대 스피너를 보이지 않게 함
                    department.setVisibility(View.GONE); // 학과 스피너를 보이지 않게 함
                }

            }
        });

        final EditText editText = (EditText) findViewById(R.id.search); // 검색할 명칭 또는 교수님 성함
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_ENTER) {
                    buttonSearch.performClick();
                    return false;
                }
                return false;
            }
        });

        buttonSearch.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sql = "";

                if (editText.getText().toString().equals("")) {
                    closeDrawer();
                    Toast.makeText(v.getContext(), "아무것도 입력되지 않았습니다.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (buttonCategory.getText().toString().equals("교수 검색")) {
                    sql = "select f.location_left, f.location_right, f.location_up, f.location_down, f.floor_start, f.floor_end from Facility as f, Professor as p where p.professor_name=\""
                            + editText.getText().toString().replaceAll(" ", "").trim()
                            + "\" and f.facility_name = p.facility_name and p.department=\""
                            + department.getSelectedItem().toString().replaceAll(" ", "").trim()
                            + "\"";
                } else {
                    sql = "select location_left, location_right, location_up, location_down, floor_start, floor_end from Facility where facility_name=\""
                            + editText.getText().toString().replaceAll(" ", "").trim() + "\"";
                }

                Cursor cursor = database.rawQuery(sql, null);
                LinearLayout info = (LinearLayout) findViewById(R.id.info);

                if (cursor.moveToFirst()) {

                    float x = (cursor.getFloat(0) + cursor.getFloat(1)) / 2;
                    float y = (cursor.getFloat(2) + cursor.getFloat(3)) / 2;
                    int start = cursor.getInt(4);
                    int end = cursor.getInt(5);

                    setInfo(start, end);
                    LatLng latLng = new LatLng(y, x);
                    onMapClick(latLng);
                    info.setVisibility(View.VISIBLE);
                } else {
                    if (marker != null) {
                        marker.remove();
                    }
                    LatLng latLng = new LatLng(35.888174, 128.611354); // 경북대학교 IT대학 융복합공학관

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng)); // 카메라 이동
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

                    info.setVisibility(View.INVISIBLE);

                    Toast.makeText(v.getContext(), "잘못된 입력입니다.", Toast.LENGTH_LONG).show();
                }
                cursor.close();

                closeDrawer();

                editText.setText("");
            }

            public void closeDrawer() {
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
                drawer.closeDrawer(GravityCompat.START);

                InputMethodManager manager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_down);
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 지도 내 요소(내 위치, 나침반 버튼 위치 조정)
//        int pxValue = 620;
//        int dpValue = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pxValue, getApplication().getResources().getDisplayMetrics());
//        mMap.setPadding(0, dpValue, 0, 0);

        LatLng latLng = new LatLng(35.888174, 128.611354); // 경북대학교 IT대학 융복합공학관

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng)); // 카메라 이동
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));    // 확대

        mMap.setOnMapClickListener(this);

        // 내 위치 버튼
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            checkLocationPermissionWithRationale();
        }

        // 경북대 지도 이미지 오버레이
        LatLng imgLatLng = new LatLng(35.890350, 128.610300);
        GroundOverlayOptions newarkMap = new GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.campus)).position(imgLatLng, 1350f);
        mMap.addGroundOverlay(newarkMap);
    }

    // GPS 위치 확인 버튼 관련 권한 설정
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermissionWithRationale() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("위치정보")
                        .setMessage("이 앱을 사용하기 위해서는 위치정보에 접근이 필요합니다. 위치정보 접근을 허용하여 주세요.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        }).create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // 애니메이션(정보 창 올림/내림)
        final Animation translateUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_up);
        final Animation translateDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_down);
        LinearLayout info = (LinearLayout) findViewById(R.id.info);
        TextView buildingName = (TextView) findViewById(R.id.bName);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng)); // 카메라 이동
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17));    // 확대

        if (marker != null) {
            marker.remove();
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        markerOptions = new MarkerOptions().position(latLng);   // 마커 좌표 설정   TODO: 좌표, 타이틀 합치기 (void getInformation() -> String getBuildingName())
        getInformation(latLng.longitude, latLng.latitude);      // 마커 타이틀 설정
        marker = mMap.addMarker(markerOptions);
        buildingName.setText(marker.getTitle());
        if (isUp && marker.getTitle().equals("")) {    // 정보 창이 올라가 있고, 건물 이름이 공백이면
            // 정보 창을 내림
            info.setVisibility(View.INVISIBLE);
            info.startAnimation(translateDown);
            isUp = false;
        } else if (!isUp && !marker.getTitle().equals("")) {    // 정보 창이 내려가 있고, 건물 이름이 유효하면
            // 정보 창을 올림
            info.setVisibility(View.VISIBLE);
            info.startAnimation(translateUp);
            isUp = true;
        }
    }

    public void getInformation(double x, double y) {
        final SQLiteDatabase database = DBHelper.getReadableDatabase(); // 데이터베이스 읽기 형식으로 불러오기


        // sqlite로 부터 단과 대학에 따른 학과 리스트 가져옮
        String sql = "select location_left, location_right, location_up, location_down, facility_name from Facility";
        String title = "";
        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext()) {
            System.out.printf("%f %f %f %f %s", cursor.getFloat(0), cursor.getFloat(1), cursor.getFloat(2), cursor.getFloat(3), cursor.getString(4));
            if (x > cursor.getFloat(0) && x < cursor.getFloat(1) && y < cursor.getFloat(2) && y > cursor.getFloat(3)) {
                title = cursor.getString(4);
            }
        }
        markerOptions.title(title);
        cursor.close();








//        if (x > 128.6091773584 && x < 128.609731905 && y < 35.891289597388 && y > 35.89053802817) {
//            markerOptions.title("농대1호관");
//        } else if (x > 128.6082664132 && x < 128.608895055 && y < 35.8915986975095 && y > 35.891404491511054) {
//            markerOptions.title("농대3호관");
//        } else if (x > 128.607890903 && x < 128.608690537 && y < 35.89116791265118 && y > 35.89090688739425) {
//            markerOptions.title("농대2호관");
//        } else if (x > 128.607851006 && x < 128.608324415 && y < 35.89059506837349 && y > 35.8902685805915) {
//            markerOptions.title("출판부");
//        } else if (x > 128.60693905 && x < 128.607270978 && y < 35.89086695941747 && y > 35.89063879916384) {
//            markerOptions.title("복현회관");
//        } else if (x > 128.607071153 && x < 128.60752478 && y < 35.89116791265118 && y > 35.89090688739425) {
//            markerOptions.title("어린이집");
//        } else if (x > 128.60871735 && x < 128.60946066 && y < 35.88991112653428 && y > 35.88974842464708) {
//            markerOptions.title("생명공학관");
//        } else if (x > 128.607231080 && x < 128.608284182 && y < 35.88999505776108 && y > 35.88980845326208) {
//            markerOptions.title("제1과학관");
//        } else if (x > 128.606298677 && x < 128.606986999 && y < 35.89046441909819 && y > 35.89028406298761) {
//            markerOptions.title("자연과학대학");
//        } else if (x > 128.605939596 && x < 128.606846854 && y < 35.8898847791729 && y > 35.88971827451106) {
//            markerOptions.title("제2과학관");
//        } else if (x > 128.605728708 && x < 128.60606063 && y < 35.890285149471424 && y > 35.8900507402411) {
//            markerOptions.title("국민체육센터(수영장)");
//        } else if (x > 128.60495019 && x < 128.605452775 && y < 35.89001379967336 && y > 35.88961071177014) {
//            markerOptions.title("제2체육관");
//        } else if (x > 128.604181408 && x < 128.604914657 && y < 35.88939612909791 && y > 35.88914324674657) {
//            markerOptions.title("제1체육관");
//        } else if (x > 128.605873547 && x < 128.606124334 && y < 35.888563325522824 && y > 35.88752244455196) {
//            markerOptions.title("청룡관");
//        } else if (x > 128.60414285 && x < 128.604536466 && y < 35.88843946357752 && y > 35.88810237339876) {
//            markerOptions.title("제1학생회관(백호관)");
//        } else if (x > 128.604778535 && x < 128.605249933 && y < 35.88718046110079 && y > 35.8870302486812) {
//            markerOptions.title("학군단");
//        } else if (x > 128.60568344 && x < 128.6065556169 && y < 35.886967501636754 && y > 35.8868376612743) {
//            markerOptions.title("생물관");
//        } else if (x > 128.606898821 && x < 128.607641123 && y < 35.88699629465899 && y > 35.88669668373603) {
//            markerOptions.title("공동실험 실습관(한국기초과학지원연구원)");
//        } else if (x > 128.607242144 && x < 128.607632070 && y < 35.88943442807318 && y > 35.8892483655019) {
//            markerOptions.title("만오원");
//        } else if (x > 128.60656253 && x < 128.606688603 && y < 35.88936679370006 && y > 35.88923070990682) {
//            markerOptions.title("동물사육장");
//        } else if (x > 128.604799322 && x < 128.605538271 && y < 35.88845766260357 && y > 35.88762946686949) {
//            markerOptions.title("소운동장");
//        } else if (x > 128.606328517 && x < 128.607046343 && y < 35.88850492572626 && y > 35.88760855135182) {
//            markerOptions.title("대운동장");
//        } else if (x > 128.60740240 && x < 128.607877828 && y < 35.88892160054141 && y > 35.88852393961826) {
//            markerOptions.title("농구장");
//        } else if (x > 128.607964664 && x < 128.60850010 && y < 35.889056326740075 && y > 35.8886529623361) {
//            markerOptions.title("족구장");
//        } else if (x > 128.608859516 && x < 128.609191104 && y < 35.889121516753896 && y > 35.88892540330016) {
//            markerOptions.title("테니스장");
//        } else if (x > 128.60969804 && x < 128.611443489 && y < 35.88911418288002 && y > 35.88877193467696) {
//            markerOptions.title("테니스장");
//        } else if (x > 128.61198663 && x < 128.61228905 && y < 35.88883739655086 && y > 35.88855137394005) {
//            markerOptions.title("일청담");
//        } else if (x > 128.610424585 && x < 128.610992543 && y < 35.893044498660835 && y > 35.89257135003503) {
//            markerOptions.title("대강당");
//        } else if (x > 128.60970105 && x < 128.610054440 && y < 35.89287256840228 && y > 35.8926009558586) {
//            markerOptions.title("DGB문화센터");
//        } else if (x > 128.611180633 && x < 128.611667118 && y < 35.8920142695842 && y > 35.89149358187437) {
//            markerOptions.title("글로벌프라자");
//        } else if (x > 128.610333055 && x < 128.610849715 && y < 35.891296931060786 && y > 35.891089415031715) {
//            markerOptions.title("인문대학");
//        } else if (x > 128.61088022 && x < 128.611204773 && y < 35.8908555514204 && y > 35.89065862740246) {
//            markerOptions.title("영선동");
//        } else if (x > 128.610364235 && x < 128.61096303 && y < 35.890261790065935 && y > 35.89009474295371) {
//            markerOptions.title("인문한국진흥관");
//        } else if (x > 128.609944805 && x < 128.610753826 && y < 35.8896941000694 && y > 35.88956942502267) {
//            markerOptions.title("대학원동");
//        } else if (x > 128.61181262 && x < 128.612266592 && y < 35.89055704157964 && y > 35.890301446726994) {
//            markerOptions.title("본관");
//        } else if (x > 128.611630573 && x < 128.611888065 && y < 35.89081616665597 && y > 35.89059506837349) {
//            markerOptions.title("학생종합서비스센터");
//        } else if (x > 128.612655512 && x < 128.61280906 && y < 35.891591907098 && y > 35.89132327795226) {
//            markerOptions.title("도서관휴게실");
//        } else if (x > 128.613412231 && x < 128.613959066 && y < 35.89165329239672 && y > 35.89127465842404) {
//            markerOptions.title("정보전산원");
//        } else if (x > 128.61192829 && x < 128.612410426 && y < 35.89194256318586 && y > 35.89145962976601) {
//            markerOptions.title("중앙도서관(구관)");
//        } else if (x > 128.612473122 && x < 128.61276715 && y < 35.89215360796801 && y > 35.8918219659149) {
//            markerOptions.title("중앙도서관(신관)");
//        } else if (x > 128.614227622 && x < 128.614850230 && y < 35.89112608347954 && y > 35.8908585392293) {
//            markerOptions.title("어학교육원");
//        } else if (x > 128.615137562 && x < 128.615675009 && y < 35.890809647796544 && y > 35.89050923644145) {
//            markerOptions.title("향토관");
//        } else if (x > 128.614431135 && x < 128.61515264 && y < 35.89180431089358 && y > 35.89123663195656) {
//            markerOptions.title("첨성관");
//        } else if (x > 128.61397247 && x < 128.61427456 && y < 35.89225247560041 && y > 35.89187764711011) {
//            markerOptions.title("IT융합 산업빌딩");
//        } else if (x > 128.60740240 && x < 128.607877828 && y < 35.88892160054141 && y > 35.88852393961826) {
//            markerOptions.title("농구장");
//        } else if (x > 128.613158091 && x < 128.6134387180 && y < 35.89247900152337 && y > 35.892166373850564) {
//            markerOptions.title("종합정보센터");
//        } else if (x > 128.613277114 && x < 128.613882288 && y < 35.8928872354531 && y > 35.8926012274716) {
//            markerOptions.title("테크노파크");
//        } else if (x > 128.612052015 && x < 128.6126595363 && y < 35.8928043937414 && y > 35.89254092936072) {
//            markerOptions.title("약학대학");
//        } else if (x > 128.61210096 && x < 128.612787947 && y < 35.893385641954666 && y > 35.8931452664587) {
//            markerOptions.title("조형관");
//        } else if (x > 128.61110989 && x < 128.61169125 && y < 35.89360754727315 && y > 35.89338998771962) {
//            markerOptions.title("예술대학");
//        } else if (x > 128.612129129 && x < 128.612692058 && y < 35.893871008104355 && y > 35.893561645244816) {
//            markerOptions.title("조소동");
//        } else if (x > 128.61233197 && x < 128.612565658 && y < 35.894507927832194 && y > 35.89431617343406) {
//            markerOptions.title("문예관");
//        } else if (x > 128.61201345 && x < 128.612253516 && y < 35.89482190428575 && y > 35.89466138533747) {
//            markerOptions.title("차고");
//        } else if (x > 128.613020293 && x < 128.614109940 && y < 35.89357685538733 && y > 35.89328188674537) {
//            markerOptions.title("누리관");
//        } else if (x > 128.613074943 && x < 128.613816909 && y < 35.89470728672817 && y > 35.89387046488704) {
//            markerOptions.title("농장");
//        } else if (x > 128.614685609 && x < 128.61483983 && y < 35.89270878614997 && y > 35.892643327476755) {
//            markerOptions.title("테크노문");
//        } else if (x > 128.609314821 && x < 128.60942982 && y < 35.892399147135286 && y > 35.892308699720886) {
//            markerOptions.title("북문");
//        } else if (x > 128.61005846 && x < 128.61104384 && y < 35.88936760857237 && y > 35.88922201792007) {
//            markerOptions.title("백향로");
//        } else if (x > 128.603869602 && x < 128.6040204763 && y < 35.88851171640251 && y > 35.88843484591355) {
//            markerOptions.title("서문");
//        } else if (x > 128.612198531 && x < 128.612435571 && y < 35.890862613513995 && y > 35.89072517419435) {
//            markerOptions.title("취업정보센터");
//        } else {
//            markerOptions.title("");
//        }
    }
}
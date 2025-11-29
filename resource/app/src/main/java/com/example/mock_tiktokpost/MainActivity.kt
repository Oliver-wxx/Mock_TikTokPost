package com.example.mock_tiktokpost

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File
import java.util.*
import com.example.mock_tiktokpost.ui.theme.Mock_TiktokPostTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Upload



//COLOR
val BackgroundColor = Color(0xFF161823) // deep black background
val SurfaceColor = Color(0xFF252735) // gray for the button
val TextColorPrimary = Color.White
val TextColorSecondary = Color(0xFF8A8B91)
val DouyinRed = Color(0xFFFE2C55)
val TagBackground = Color(0xFF2F313D)
//DATA
data class MockUser(val id: String, val name: String)
//PostView
class PostViewModel : ViewModel() {
    private val MAX_TITLE_LENGTH = 20    // max length of title
    val MAX_DESC_LENGTH = 200    // max length of description
    private val MAX_IMG_COUNT = 9        // max num of image
    private val _title = mutableStateOf("")
    private val _description = mutableStateOf("")
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    var currentLatitude by mutableStateOf<Double?>(null)
    var currentLongitude by mutableStateOf<Double?>(null)

    var title: String by _title
    var description: String by _description
    var selectedImages = mutableStateListOf<Uri>()
    var locationName by mutableStateOf("未获取定位")
    var isLocationLoading by mutableStateOf(false)
    // Mock Data
    val trendingTopics = listOf("#男大学生","#抽象","#日常分享","#生活碎片","#王者荣耀","#西安","#内容太过真实","#上热门🔥上热门","#热门挑战", "#今日穿搭", "#美食分享", "#旅行Vlog",
        "#搞笑日常", "#音乐推荐", "#学习打卡", "#生活记录")
    val mockFriends = listOf(
        MockUser("1", "张三"), MockUser("2", "李四"), MockUser("3", "王五"),
        MockUser("4", "赵六"), MockUser("5", "孙七")
    )

    fun updateTitle(input: String) {
        _title.value = input.take(MAX_TITLE_LENGTH)
    }

    fun updateDescription(input: String) {
        _description.value = input.take(MAX_DESC_LENGTH)
    }
    // PhotoManage
    fun removeImage(uri: Uri) {
        selectedImages.remove(uri)
    }

    fun addImage(uri: Uri) {
        if (selectedImages.size < MAX_IMG_COUNT) {
            selectedImages.add(uri)
        }
    }
    fun moveImage(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val tempUri = selectedImages.removeAt(fromIndex)
        selectedImages.add(toIndex, tempUri)
    }
    //AddTag
    fun appendText(text: String) {
        val newDesc = "${_description.value} $text ".take(MAX_DESC_LENGTH)
        _description.value = newDesc
    }

    fun validateBeforePublish(): Pair<Boolean, String> {
        return when {
            selectedImages.isEmpty() -> Pair(false, "请至少添加一张图片")
            title.isEmpty() -> Pair(false, "标题不能为空")
            title.length > MAX_TITLE_LENGTH -> Pair(false, "标题不能超过${MAX_TITLE_LENGTH}字")
            description.length > MAX_DESC_LENGTH -> Pair(false, "描述不能超过${MAX_DESC_LENGTH}字")
            selectedImages.size > MAX_IMG_COUNT -> Pair(false, "最多只能选择9张图片")
            else -> Pair(true, "校验通过")
        }
    }



    //GetLocation

    fun updateLocation(context: Context) {
        isLocationLoading = true

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            locationName = "定位权限未授予"
            isLocationLoading = false
            currentLatitude = null
            currentLongitude = null
            return
        }

        if (!::fusedLocationClient.isInitialized) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }

        val locationRequest = LocationRequest().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2000
            maxWaitTime = 10000
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val latestLocation = locationResult.lastLocation
                latestLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    locationName = "已获取定位"
                } ?: run {
                    locationName = "未获取到实时位置"
                    currentLatitude = null
                    currentLongitude = null
                }
                stopLocationUpdates()
                isLocationLoading = false
            }
        }

        // 4. 开始请求实时位置更新
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            null
        ).addOnFailureListener { exception ->
            exception.printStackTrace()
            stopLocationUpdates()
            isLocationLoading = false
            locationName = "定位失败：${exception.message ?: "未知错误"}"
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isLocationLoading) {
                stopLocationUpdates()
                locationName = "定位超时"
                currentLatitude = null
                currentLongitude = null
                isLocationLoading = false
            }
        }, 10000)
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.setDecorFitsSystemWindows(false) // 关闭系统自动适配
//            window.navigationBarColor = Color.TRANSPARENT // 导航栏透明（避免与底部栏重叠）
//            // 导航栏按钮颜色设为白色（确保可见）
//            window.isNavigationBarContrastEnforced = false
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
//            window.navigationBarColor = Color.TRANSPARENT
//        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BackgroundColor,
                    surface = SurfaceColor,
                    primary = DouyinRed
                )
            ) {
                TiktokPostScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TiktokPostScreen(viewModel: PostViewModel = viewModel()) {
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        val isShowFriendSheet = remember { mutableStateOf(false) }
        val isShowTopicSheet = remember { mutableStateOf(false) }
        val isShowPermissionSheet = remember { mutableStateOf(false) }
        val currentPermission = remember { mutableStateOf("公开 · 所有人可见") }
        val isShowFriendSelectSheet = remember { mutableStateOf(false) }
        var selectType by remember { mutableStateOf("") }
        val selectedFriends = remember { mutableStateListOf<MockUser>() }

        // Camera Logic
        var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
        val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && tempPhotoUri != null) {
                    viewModel.addImage(tempPhotoUri!!)
                }
            }
        fun launchCamera(context: Context) {
            try {
                val cacheDir = context.cacheDir
                val fileName = "temp_${System.currentTimeMillis()}.jpg"
                val file = File(cacheDir, fileName)

                val authority = "com.example.mock_tiktokpost.provider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "相机启动失败：${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                launchCamera(context)
            } else {
                Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }


        // Gallery Logic
        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris.forEach { uri ->
                viewModel.addImage(uri)
            }
        }

        // Location Permission Logic
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                viewModel.updateLocation(context)
            }
        }
        //Run when project first start and only run once
        LaunchedEffect(Unit) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // Dialog for adding content
        val (showAddOptions,setShowAddOptions) = remember { mutableStateOf(false) }

        if (showAddOptions) {
            // choose a photo from album
            AlertDialog(
                onDismissRequest = { setShowAddOptions(false) },
                title = { Text("选择图片来源") },
                //choose photo from album
                confirmButton = {
                    TextButton(onClick = {
                        setShowAddOptions(false)
                        galleryLauncher.launch("image/*")
                    }) { Text("相册",color = TextColorPrimary) }
                },
                // take a photo by camera
                dismissButton = {
                    TextButton(onClick = {
                        setShowAddOptions(false)
                        // 检查相机权限，有则启动，无则申请
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera(context) // 已有权限，直接启动
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA) // 申请权限
                        }
                    }) { Text("拍照",color = TextColorPrimary) }
                },
                containerColor = SurfaceColor
            )
        }

        Scaffold(
            containerColor = BackgroundColor,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            (context as ComponentActivity).finish()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "返回",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        Text(
                            "预览",
                            color = Color.White,
                            modifier = Modifier.padding(end = 16.dp),
                            fontSize = 16.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
                )
            },
            bottomBar = {
                BottomBarSection(

                    viewModel = viewModel,
                    context = context
                )
            },contentWindowInsets = WindowInsets.navigationBars
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                // 1. Preview Cover Image (Use first image on default)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(horizontal = 50.dp, vertical = 10.dp)
                ) {
                    val coverImage = viewModel.selectedImages.firstOrNull()
                    if (coverImage != null) {
                        AsyncImage(
                            model = coverImage,
                            contentDescription = "封面图",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("编辑封面", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2A2C36), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("点击添加图片", color = Color.Gray)
                            }
                        }
                    }
                }

                // 2. Image List
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                ) {
                    itemsIndexed(viewModel.selectedImages) { index, uri ->
                        DraggableImageItem(
                            uri = uri,
                            index = index,
                            totalCount = viewModel.selectedImages.size,
                            isFirstImage = index == 0,
                            onRemove = { viewModel.removeImage(uri) },
                            onMove = { from, to -> viewModel.moveImage(from, to) },
                            douyinRed = DouyinRed
                        )
                    }

                    // add photo button
                    item {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color(0xFF252735), RoundedCornerShape(4.dp))
                                .clickable { setShowAddOptions(true) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "添加",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Text Fields (Title & Description)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Title
                    BasicTextField(
                        value = viewModel.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        textStyle = TextStyle(
                            color = TextColorPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        decorationBox = { innerTextField ->
                            if (viewModel.title.isEmpty()) Text(
                                "添加标题",
                                color = TextColorSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    BasicTextField(
                        value = viewModel.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        textStyle = TextStyle(color = TextColorPrimary, fontSize = 15.sp),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (viewModel.description.isEmpty()) {
                                    Text(
                                        "添加作品描述...",
                                        color = TextColorSecondary,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()

                                val currentLength = viewModel.description.length
                                Text(
                                    text = "$currentLength/${viewModel.MAX_DESC_LENGTH}",
                                    color = if (currentLength > 190) DouyinRed else TextColorSecondary, // turn red when bryond 190
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 4.dp, end = 2.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        maxLines = 5,
                        singleLine = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Tag Buttons
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TagButton(text = "# 话题") { viewModel.appendText("#") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TagButton(text = "@ 朋友") { isShowFriendSheet.value = true }
                }


                // 5. Trending Tags List
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(viewModel.trendingTopics) { _, topic ->
                        Row(
                            modifier = Modifier
                                .background(TagBackground, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable { viewModel.appendText(topic) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topic,
                                color = TextColorSecondary,
                                fontSize = 13.sp
                            )
                            // Fire Icon logic simulation
                            if (topic.contains("热门")) {
                                Text(
                                    "🔥",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF252735), thickness = 0.5.dp)

                // 6. Option List Items
                OptionItem(
                    icon = Icons.Outlined.LocationOn,
                    label = if (viewModel.isLocationLoading) "定位中..." else viewModel.locationName,
                    onClick = { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) }
                )
                OptionItem(
                    icon = Icons.Filled.GridView,
                    label = "添加标签",
                    onClick = { isShowTopicSheet.value = true }
                )
                OptionItem(
                    icon = Icons.Filled.Lock,
                    label = currentPermission.value, // 显示当前选中的权限
                    onClick = { isShowPermissionSheet.value = true } // 打开权限选择弹窗
                )
                OptionItem(
                    icon = Icons.Filled.Settings,
                    label = "高级设置",
                    onClick = {}
                )

                // Footer Note
                Text(
                    "发布成功后将保存内容至本地",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (isShowFriendSheet.value) {
                    AlertDialog(
                        onDismissRequest = { isShowFriendSheet.value = false },
                        title = { Text("选择要@的朋友", color = TextColorPrimary) },
                        containerColor = SurfaceColor,
                        text = {
                            Column(
                                modifier = Modifier.height(200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                viewModel.mockFriends.forEach { mockUser ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.appendText("@${mockUser.name} ")
                                                isShowFriendSheet.value = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3A3C48)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mockUser.name.first().toString(),
                                                color = TextColorPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = mockUser.name,
                                            fontSize = 16.sp,
                                            color = TextColorPrimary
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { isShowFriendSheet.value = false }) {
                                Text("取消", color = TextColorPrimary)
                            }
                        }
                    )
                }

                if (isShowTopicSheet.value) {
                    AlertDialog(
                        onDismissRequest = { isShowTopicSheet.value = false },
                        containerColor = SurfaceColor,
                        title = {
                            Text(
                                text = "选择热门标签",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextColorPrimary
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .height(200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                viewModel.trendingTopics.forEach { topic ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.appendText("$topic ")
                                                isShowTopicSheet.value = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (topic.contains("热门")) {
                                            Text(
                                                "🔥",
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(TagBackground),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#",
                                                    color = TextColorSecondary,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = topic,
                                            fontSize = 16.sp,
                                            color = TextColorPrimary
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { isShowTopicSheet.value = false }) {
                                Text("取消", color = TextColorPrimary)
                            }
                        }
                    )
                }

                if (isShowPermissionSheet.value) {
                    AlertDialog(
                        onDismissRequest = { isShowPermissionSheet.value = false },
                        containerColor = SurfaceColor,
                        title = {
                            Text(
                                text = "选择可见范围",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextColorPrimary
                            )
                        },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                PermissionItem(
                                    label = "公开 · 所有人可见",
                                    isSelected = currentPermission.value == "公开 · 所有人可见",
                                    onClick = {
                                        currentPermission.value = "公开 · 所有人可见"
                                        isShowPermissionSheet.value = false
                                    }
                                )
                                PermissionItem(
                                    label = "互相关注的人可见",
                                    isSelected = currentPermission.value == "互相关注的人可见",
                                    onClick = {
                                        currentPermission.value = "互相关注的人可见"
                                        isShowPermissionSheet.value = false
                                    }
                                )
                                PermissionItem(
                                    label = "密友可见",
                                    isSelected = currentPermission.value == "密友可见",
                                    onClick = {
                                        currentPermission.value = "密友可见"
                                        isShowPermissionSheet.value = false
                                    }
                                )
                                PermissionItem(
                                    label = "私密 · 仅自己可见",
                                    isSelected = currentPermission.value == "私密 · 仅自己可见",
                                    onClick = {
                                        currentPermission.value = "私密 · 仅自己可见"
                                        isShowPermissionSheet.value = false
                                    }
                                )
                                PermissionItem(
                                    label = "部分可见",
                                    isSelected = currentPermission.value.startsWith("部分可见"),
                                    onClick = {
                                        selectType = "部分可见"
                                        isShowPermissionSheet.value = false
                                        isShowFriendSelectSheet.value = true
                                    }
                                )
                                PermissionItem(
                                    label = "不给谁看",
                                    isSelected = currentPermission.value.startsWith("不给谁看"),
                                    onClick = {
                                        selectType = "不给谁看"
                                        isShowPermissionSheet.value = false
                                        isShowFriendSelectSheet.value = true // 打开好友选择
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { isShowPermissionSheet.value = false }) {
                                Text("取消", color = TextColorPrimary)
                            }
                        }
                    )
                }

                if (isShowFriendSelectSheet.value) {
                    AlertDialog(
                        onDismissRequest = { isShowFriendSelectSheet.value = false },
                        containerColor = SurfaceColor,
                        title = {
                            Text(
                                text = if (selectType == "部分可见") "选择可见的好友" else "选择不可见的好友",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextColorPrimary
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .height(200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                viewModel.mockFriends.forEach { friend ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                // 切换好友选中状态
                                                if (selectedFriends.contains(friend)) {
                                                    selectedFriends.remove(friend)
                                                } else {
                                                    selectedFriends.add(friend)
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 好友头像
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3A3C48)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = friend.name.first().toString(),
                                                color = TextColorPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        // 好友昵称
                                        Text(
                                            text = friend.name,
                                            fontSize = 16.sp,
                                            color = TextColorPrimary
                                        )
                                        // 选中状态图标
                                        if (selectedFriends.contains(friend)) {
                                            Icon(
                                                imageVector = Icons.Filled.Check, // 第一个参数：imageVector
                                                contentDescription = "已选中",
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .align(Alignment.CenterVertically),
                                                tint = DouyinRed // tint 放在最后
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                // 确认选择，更新权限状态
                                currentPermission.value = "$selectType（${selectedFriends.size}人）"
                                isShowFriendSelectSheet.value = false
                            }) {
                                Text("确定", color = TextColorPrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { isShowFriendSelectSheet.value = false }) {
                                Text("取消", color = TextColorPrimary)
                            }
                        }
                    )
                }
//                Spacer(modifier = Modifier.height(60.dp)) // Bottom padding
            }
        }
    }
}

@Composable
fun PermissionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = TextColorPrimary
        )
        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "选中",
                tint = DouyinRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
@Composable
fun BottomBarSection(
    viewModel: PostViewModel,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(BackgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 分享按钮
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        Toast.makeText(context, "分享", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.share),
                    contentDescription = "分享",
                    tint = TextColorPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "分享",
                    color = TextColorPrimary,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                )
            }

            // 2. 限时日常按钮
            Button(
                onClick = {
                    Toast.makeText(context, "限时日常", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2C36)),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "头像",
                        tint = TextColorPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "限时日常",
                        color = TextColorPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            // 3. 发作品按钮
            Button(
                onClick = {
                    val (isValid, message) = viewModel.validateBeforePublish()
                    if (isValid) {
                        Toast.makeText(context, "发布成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.selectedImages.isNotEmpty()) DouyinRed else Color(0xFF8A2C40),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(22.dp),
                enabled = viewModel.selectedImages.isNotEmpty()
            )
            {
                Icon(
                    imageVector = Icons.Filled.Upload,
                    contentDescription = "发作品",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "发作品",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
fun TagButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F313D)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
fun OptionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextColorSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = TextColorPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF464850),
            modifier = Modifier.size(14.dp)
        )
    }
}


@Composable
private fun DraggableImageItem(
    uri: Uri,
    index: Int,
    totalCount: Int,
    isFirstImage: Boolean,
    onRemove: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    douyinRed: Color
) {
    var isDragging by remember { mutableStateOf(false) }
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(70.dp)
            .pointerInput(index) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change: PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        val threshold = 35.dp.value
                        val newIndex = when {
                            offsetX.value > threshold && index < totalCount - 1 -> index + 1
                            offsetX.value < -threshold && index > 0 -> index - 1
                            else -> index
                        }
                        if (newIndex != index) {
                            onMove(index, newIndex)
                        }
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                        }
                    }
                )
            }
            .background(if (isDragging) Color.LightGray.copy(0.3f) else Color.Transparent)
            .padding(if (isDragging) 2.dp else 0.dp)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(4.dp))
                .border(
                    1.dp,
                    // 👇 不再引用 viewModel，用传入的 isFirstImage 判断
                    if (isFirstImage) douyinRed else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "删除",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .padding(2.dp)
                .clickable { onRemove() }
        )
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Mock_TiktokPostTheme {
        Greeting("Android")
    }
}
# Mock_TikTokPost
## 项目介绍

本项目是一个高仿抖音（TikTok）投稿页面的 Android 应用，实现了抖音发布图文内容的核心功能。通过该应用，用户可以选择或拍摄图片，添加文字描述、话题标签、位置信息，并设置内容可见范围等，模拟了抖音完整的内容发布流程。

技术栈：
* 开发语言：Kotlin
* 开发工具：Android Studio
* UI 框架：Jetpack Compose
* 架构组件：ViewModel
* 其他：Coil 图片加载等
## 整体框架
项目采用 MVVM 架构模式，主要分为以下几个部分：
* UI 层：使用 Jetpack Compose 构建的界面组件，包括顶部导航栏、内容编辑区、图片预览区、底部操作栏等
* 数据层：通过PostViewModel管理所有投稿相关数据（图片、文字、位置等）和业务逻辑
* 交互层：通过 Launcher 处理系统功能交互（相机、相册、权限请求）
## 代码解析
### 核心组件
* MainActivity：应用入口，负责设置 Compose 内容和协调各功能模块
* PostViewModel：数据管理和业务逻辑处理中心
* Compose UI 组件：构建用户界面的各种可组合函数
### 关键代码
#### 1.PostViewModel 核心功能
```
class PostViewModel : ViewModel() {
    private val MAX_TITLE_LENGTH = 20    // max length of title
     val MAX_DESC_LENGTH = 200    // max length of description
    private val MAX_IMG_COUNT = 9        // max num of image
    private val _title = mutableStateOf("")
    private val _description = mutableStateOf("")

    var title: String by _title
    var description: String by _description
    var selectedImages = mutableStateListOf<Uri>()
    // 图片管理
    fun removeImage(uri: Uri) { ... }
    fun addImage(uri: Uri) { ... }
    fun moveImage(fromIndex: Int, toIndex: Int) { ... }
    
    // 内容验证
    fun validateBeforePublish(): Pair<Boolean, String> { ... }
    
    //增加标签
    fun appendText(text: String) { ... }
    }
```
#### 2. 图片选择与拍摄功能
```
// 相机功能
val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    if (success && tempPhotoUri != null) {
        viewModel.addImage(tempPhotoUri!!)
    }
}

// 相册功能
val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
    uris.forEach { uri ->
        viewModel.addImage(uri)
    }
}

if (showAddOptions) {
            AlertDialog(
            // choose a photo from album
                onDismissRequest = { setShowAddOptions(false) },
                title = { Text("选择图片来源") },
                //choose photo from album
                confirmButton = {
                    TextButton(onClick = {
                        setShowAddOptions(false)
                        galleryLauncher.launch("image/*")
                    }) { Text("相册") }
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
                    }) { Text("拍照") }
                }
            )
        }
```
#### 3.权限管理（在申请相机时和位置时需要申请权限）
```
// 相机权限请求
val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        launchCamera(context)
    } else {
        Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
    }
}

// 位置权限请求
val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    ) {
        viewModel.updateLocation(context)
    }
}
```

#### 4.图片列表
```
LazyRow(
                    ...
                ) {
                    itemsIndexed(viewModel.selectedImages) { index, uri ->
                        DraggableImageItem(  //move photo 
                            uri = uri,
                            index = index,
                            totalCount = viewModel.selectedImages.size,
                            isFirstImage = index == 0,
                            onRemove = { viewModel.removeImage(uri) },
                            onMove = { from, to -> viewModel.moveImage(from, to) },
                            douyinRed = DouyinRed
                        )
                    }

                    // Add photo button
                    item { ...
                    }
                }
```
#### 5.文案，限制字数
```
BasicTextField(
                        value = viewModel.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        textStyle = TextStyle(color = TextColorPrimary, fontSize = 15.sp),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (viewModel.description.isEmpty()) {
                                    Text(
                                        "添加作品描述...",
                                        ...
                                    )
                                }
                                innerTextField()

                                val currentLength = viewModel.description.length
                                Text(
                                    text = "$currentLength/${viewModel.MAX_DESC_LENGTH}", 
                                    color = if (currentLength > 190) DouyinRed else TextColorSecondary, // turn to red when beyond 190
                                    ...
                                )
                            }
                        },
                        ...
                    )
```
#### 6.@朋友，选择标签，修改可见范围
```
//@朋友
if (isShowFriendSheet.value){
...
}

//选择标签（从Mock数据中）
 if (isShowTopicSheet.value){
 ...
}

//修改可见范围
if (isShowPermissionSheet.value){
...
}

```
#### 7.底部button实现
```
    @Composable
fun BottomBarSection(
    viewModel: PostViewModel,
    context: Context
) {
...
}
```
#### 8.定位功能
```
fun updateLocation(context: Context) {
...
}
```

# 作者
oliver-wxx https://github.com/Oliver-wxx

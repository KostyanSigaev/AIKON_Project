# AIKON_Project
# AIKON UI — техническая документация

Документ описывает архитектуру проекта AIKON UI и назначение каждого компонента. Предназначен для участников проекта, разрабатывающих новые приложения (контакты, сообщения, настройки и т.п.) поверх модуля `core`.

## Содержание

1. [Общая архитектура](#общая-архитектура)
2. [Схема тем (AIKON Themes) — план](#схема-тем-aikon-themes--план)
3. [Модуль com.aikon.core](#модуль-comaikoncore)
   - [Система тем](#система-тем)
   - [AikonBaseActivity](#aikonbaseactivity)
   - [AikonSoftkeyBar — нижняя панель кнопок](#aikonsoftkeybar--нижняя-панель-кнопок)
   - [AikonScreenLock — блокировка экрана](#aikonscreenlock--блокировка-экрана)
   - [Списки: AikonListView / AikonListAdapter / AikonListItem](#списки-aikonlistview--aikonlistadapter--aikonlistitem)
   - [AikonIconRegistry — реестр иконок](#aikoniconregistry--реестр-иконок)
   - [AikonLoadingOverlay — окно загрузки](#aikonloadingoverlay--окно-загрузки)
   - [AikonContextMenu — контекстное меню](#aikoncontextmenu--контекстное-меню)
   - [Общие drawable-ресурсы](#общие-drawable-ресурсы)
4. [Модуль com.aikon.launcher](#модуль-comaikonlauncher)
   - [Манифест и точка входа](#манифест-и-точка-входа)
   - [MainActivity](#mainactivity)
   - [Пакет apps — кэш установленных приложений](#пакет-apps--кэш-установленных-приложений)
   - [Пакет favorites — избранные приложения](#пакет-favorites--избранные-приложения)
   - [Пакет home — главный экран](#пакет-home--главный-экран)
   - [Пакет mainmenu — главное меню](#пакет-mainmenu--главное-меню)
   - [Пакет applist — списки приложений](#пакет-applist--списки-приложений)
   - [Пакет games — список игр](#пакет-games--список-игр)
   - [Пакет multimedia — список мультимедиа](#пакет-multimedia--список-мультимедиа)
5. [Модуль com.aikon.filemanager](#модуль-comaikonfilemanager)
6. [Как сделать новое приложение проекта](#как-сделать-новое-приложение-проекта)
7. [Типичные ошибки настройки](#типичные-ошибки-настройки)
8. [Известные ограничения и TODO](#известные-ограничения-и-todo)

---

## Общая архитектура

Проект состоит из Android-модулей:

| Модуль | Тип | Назначение |
|---|---|---|
| `com.aikon.core` | Android Library (`com.android.library`) | Весь общий UI-слой: стили, темы, переиспользуемые View и системные компоненты (панель кнопок, списки, меню, окно загрузки, блокировка экрана). Не содержит логики конкретных приложений. |
| `com.aikon.launcher` | Android Application | Домашний экран: главный экран, главное меню, списки приложений, избранное. |
| `com.aikon.filemanager` | Android Application | Файловый менеджер. |

### Как на самом деле устроено ядро

**Ядро не отдельный пакет на устройстве.** Library-модуль при сборке копируется внутрь каждого APK, который его подключает. У лаунчера и у файлового менеджера по собственной копии `core`.

Из этого следует:

- Изменение `core` попадает в приложение только после его пересборки и обновления. Обновить стили в уже установленном APK отдельно от самого приложения нельзя.
- Каждое приложение содержит код и ресурсы ядра, поэтому ядро не уменьшает вес отдельного APK. Выигрыш — в отсутствии дублирования **исходников** и в единообразии компонентов на момент сборки.
- Всё, что объявлено в манифесте `core`, попадает в манифест каждого приложения. Поэтому в манифест ядра нельзя помещать компоненты, которые нужны только одному приложению (например, приёмник администратора устройства — см. [AikonScreenLock](#aikonscreenlock--блокировка-экрана)).

Единая смена оформления на устройстве без пересборки приложений предполагается через отдельное приложение AIKON Themes (см. следующий раздел).

**Принцип проекта:** внешний вид элементов (XML) и логика их получения (Java) живут в `core`; приложения подключают модуль и используют готовые классы, не описывая собственных стилей кнопок, списков и попапов.

Зависимости ядра подключены через `api` (а не `implementation`): публичные классы `core` (`AikonListAdapter`, `AikonBaseActivity` и т.п.) наследуются от типов AndroidX, и эти типы должны быть видны любому модулю, подключившему `core`.

```gradle
dependencies {
    api 'androidx.appcompat:appcompat:1.6.1'
    api 'com.google.android.material:material:1.11.0'
    api 'androidx.constraintlayout:constraintlayout:2.1.4'
    api 'androidx.recyclerview:recyclerview:1.3.2'
}
```

### Планируемое распространение

Проект гибридный: набор APK для любой прошивки; при желании прошивка может сделать его системным, заменив системный интерфейс на интерфейс проекта. Для этого позднее будет создано приложение-заглушка системного интерфейса, содержащее ядро и подхватывающее тему по той же схеме, что и остальные приложения. На данном этапе не реализовано.

---

## Схема тем (AIKON Themes) — план

**Статус: спроектировано, не реализовано.** Приложение `com.aikon.themes` пока не существует; раздел фиксирует принятую схему.

- В каждом приложении ядро содержит **стандартную тему**, которая работает всегда.
- Приложение AIKON Themes (`com.aikon.themes`): набор предустановленных пресетов и импорт темы.
- При запуске ядро проверяет наличие `com.aikon.themes`. Если менеджер тем есть и его активная тема отличается от закэшированной в приложении — берётся тема из менеджера; если менеджера нет — остаётся стандартная.

Вопросы, которые нужно закрыть при реализации:

1. **Видимость пакета.** На Android 11+ без записи `<queries><package android:name="com.aikon.themes"/></queries>` приложение не увидит менеджер тем. Запись можно разместить в манифесте `core`: она ничего не открывает приложениям, в отличие от компонентов.
2. **Доверие к источнику.** Имя пакета не защищает от подмены: поставщик темы должен проверяться по подписи (сертификат пакета `com.aikon.themes` или разрешение уровня `signature` на провайдере).
3. **Формат передачи.** Вариант: `ContentProvider` в менеджере тем либо чтение ресурсов пакета по имени (как в пакетах иконок для лаунчеров). Формат ещё не выбран.
4. **Чтение значений в Java.** Сейчас оформление частично задано XML-атрибутами `?attr/...` и селекторами, которые фиксируются при сборке. Для динамических тем значения придётся читать из Java-слоя (`AikonTheme`) с запасными значениями из самого `core`.
5. **Применение.** Смена темы применяется после пересоздания Activity; закэшированная версия сравнивается с версией менеджера при каждом запуске.

---

## Модуль com.aikon.core

### Система тем

Все визуальные параметры общих компонентов вынесены в атрибуты темы. Чтобы создать новую тему оформления **на этапе сборки**, достаточно объявить `<style>`, наследующий `Theme.Aikon`, и переопределить нужные атрибуты. (Смена темы на устройстве без пересборки — см. [схему тем](#схема-тем-aikon-themes--план).)

**Расположение:** `core/res/values/attrs.xml`, `colors.xml`, `themes.xml`, `dimens.xml`.

| Атрибут | Назначение |
|---|---|
| `aikonBarBackground` | Фон нижней панели кнопок |
| `aikonBarButtonTextColor` | Цвет текста кнопок панели |
| `aikonBarButtonPressedColor` | Цвет подсветки кнопки панели при нажатии |
| `aikonBarDividerColor` | Цвет разделителей между кнопками панели |
| `aikonTextPrimaryColor` | Основной цвет текста (заголовки списков, часы и т.п.) |
| `aikonTextSecondaryColor` | Вторичный цвет текста (описания, дата) |
| `aikonListItemPressedColor` | Цвет подсветки элемента списка/меню при нажатии |

Базовая тема — `Theme.Aikon` (`parent="Theme.AppCompat.NoActionBar"`). Она задаёт поведения, обязательные для всех приложений проекта:

```xml
<item name="android:windowBackground">@android:color/transparent</item>
<item name="android:colorBackgroundCacheHint">@null</item>
<item name="android:windowFullscreen">false</item>
<item name="android:windowShowWallpaper">true</item>
```

- прозрачный фон окна + показ системных обоев (`windowShowWallpaper`);
- `windowFullscreen = false` — приложения никогда не работают в полноэкранном режиме;
- `colorBackgroundCacheHint = null` — отключает лишнее кэширование фона (важно для слабых GPU).

Каждое приложение указывает `Theme.Aikon` (или наследника) через `android:theme` в манифесте — не через `setTheme()` в коде.

### AikonBaseActivity

`core/src/main/java/com/aikon/core/ui/AikonBaseActivity.java`

Базовый класс Activity для всех приложений проекта (вместо `AppCompatActivity`).

Отвечает за:

1. **Единую точку навигации по аппаратным клавишам.** MENU / OK (DPAD_CENTER/ENTER) / BACK не обрабатываются напрямую — они «нажимают» соответствующую кнопку текущей панели `AikonSoftkeyBar` через `dispatchKeyEvent`. Тап пальцем и физическая клавиша идут через один и тот же листенер.
   - MENU → левая кнопка;
   - OK/ENTER → центральная кнопка (только в `THREE_BUTTON`; в `TWO_BUTTON` событие уходит дальше как обычное нажатие, например выбор элемента списка);
   - BACK → правая кнопка, через `OnBackPressedCallback` (единый путь для аппаратной кнопки, жеста и экранной кнопки).
2. **Управление `AikonLoadingOverlay`** — привязывает/отвязывает текущую Activity в `onResume`/`onPause`, принудительно скрывает окно при уходе Activity с переднего плана.

```java
public class MainActivity extends AikonBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AikonSoftkeyBar bar = findViewById(R.id.softkey_bar);
        attachSoftkeyBar(bar); // обязательный вызов — иначе физические клавиши не будут работать
    }
}
```

### AikonSoftkeyBar — нижняя панель кнопок

`core/src/main/java/com/aikon/core/ui/AikonSoftkeyBar.java`

`LinearLayout` с принудительно зафиксированной высотой (через `onMeasure`, не зависит от `layout_height` снаружи).

| Режим (`Mode`) | Кнопки | Где используется |
|---|---|---|
| `TWO_BUTTON` | Меню (лев.) / Назад (прав.) | Большинство экранов |
| `THREE_BUTTON` | Блокировка (лев.) / Меню (центр.) / Избранные (прав.) | Главный экран лаунчера |

**XML-атрибуты:** `app:aikonBarMode` (`twoButton` / `threeButton`), `app:aikonLeftText`, `app:aikonCenterText`, `app:aikonRightText`.

**Java API:**

```java
softkeyBar.setMode(AikonSoftkeyBar.Mode.TWO_BUTTON);
softkeyBar.setOnSoftkeyClickListener(new AikonSoftkeyBar.OnSoftkeyClickListener() {
    @Override public void onLeftClick(View v) { /* ... */ }
    @Override public void onCenterClick(View v) { /* только в THREE_BUTTON */ }
    @Override public void onRightClick(View v) { /* ... */ }
});

// Программный "клик" — используется AikonBaseActivity для проброса физических клавиш
softkeyBar.performLeftAction();
boolean handled = softkeyBar.performCenterAction(); // false, если центральной кнопки нет
softkeyBar.performRightAction();
```

**Блокировка экрана выполняется внутри панели:** левая кнопка в режиме `THREE_BUTTON` сама вызывает `performLockScreen()` → `AikonScreenLock.lock(context)`, а затем листенер. Повторять вызов блокировки в `onLeftClick` приложения **не нужно** (иначе без прав администратора откроется два окна запроса).

Кнопки панели объявлены с `focusable="false"` — они не участвуют в D-pad навигации по содержимому экрана, только в проброс из `AikonBaseActivity`.

### AikonScreenLock — блокировка экрана

`core/src/main/java/com/aikon/core/ui/lock/AikonScreenLock.java`

Блокировка через права администратора устройства (политика `force-lock`, `DevicePolicyManager.lockNow()`).

**Важное правило: ядро само не объявляет компонент администратора.** Манифест библиотеки сливается с манифестом каждого приложения, поэтому приёмник в `core` делал бы администратором любое приложение с ядром. Компонент объявляет только то приложение, которому нужна блокировка (сейчас — лаунчер), и передаёт его в ядро:

```java
AikonScreenLock.setAdminComponent(new ComponentName(this, LauncherDeviceAdminReceiver.class));
```

Если компонент не задан, нажатие «Блокировка» ничего не делает (в режиме отладки — показывает причину). Приложения без компонента в списке администраторов не появляются.

**Поведение `lock(context)`:**
1. Проверяет, что приёмник правильно объявлен в манифесте (`permission=BIND_DEVICE_ADMIN`, `exported=true`, `meta-data android.app.device_admin`).
2. Если права администратора выданы и есть политика `force-lock` — `lockNow()`.
3. Если прав нет — открывает системное окно выдачи прав; блокировка в этот раз не выполняется, после выдачи нужно нажать кнопку ещё раз.

Константа `DEBUG_TOASTS` (по умолчанию `true`) показывает Toast с причиной любого сбоя; после отладки устанавливается в `false` (причина остаётся в логкате по тегу `AikonScreenLock`).

Блокировка без прав администратора (служба специальных возможностей, API 28+) не реализована.

### Списки: AikonListView / AikonListAdapter / AikonListItem

Реализованы поверх `RecyclerView`.

#### AikonListItem

`core/src/main/java/com/aikon/core/ui/list/AikonListItem.java`

| Тип | Внешний вид | Layout |
|---|---|---|
| `SIMPLE` | Одна строка на всю ширину, только заголовок | `aikon_list_item_simple.xml` |
| `FULL` | Заголовок + описание слева, квадратная иконка справа | `aikon_list_item_full.xml` |

```java
AikonListItem<Intent> simple = AikonListItem.simple("Пункт меню", someIntent);
AikonListItem<Intent> full = AikonListItem.full("Название", "Описание", iconDrawable, someIntent);
```

`payload` (generic `T`) — произвольные данные для обработки клика (Intent, имя пакета, `File` и т.п.).

#### AikonListAdapter

`core/src/main/java/com/aikon/core/ui/list/AikonListAdapter.java`

Единый адаптер для обоих видов элементов — тип выбирается по `AikonListItem.getType()`, в одном списке можно смешивать `SIMPLE` и `FULL`.

```java
new AikonListAdapter<>(items, onItemClickListener);
new AikonListAdapter<>(items, onItemClickListener, onItemLongClickListener); // с контекстным меню
```

**Если `OnItemLongClickListener` не передан — долгое нажатие на элементах не работает.** Это единственный переключатель «есть контекстное меню / нет контекстного меню» для конкретного списка.

#### AikonListView

`core/src/main/java/com/aikon/core/ui/list/AikonListView.java`

Обёртка над `RecyclerView` с настроенным `LinearLayoutManager`, `setHasFixedSize(true)` и увеличенным `ItemViewCacheSize`. Использовать вместо голого `RecyclerView`.

При каждом `setAdapter(...)` автоматически ставит клавиатурный фокус на первый элемент (если фокус ещё не внутри списка) — на кнопочном управлении не нужно лишнее нажатие стрелки.

```xml
<com.aikon.core.ui.list.AikonListView
    android:id="@+id/list_generic"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

#### Состояния фокуса и нажатия

Все элементы используют общие drawable из `core`:
- `android:background="@drawable/aikon_list_item_bg"` — подсветка при нажатии (`state_pressed`);
- `android:foreground="@drawable/aikon_list_item_focus_overlay"` — белая рамка со скруглёнными углами при клавиатурном фокусе (`state_focused`), рисуется **поверх** содержимого (в том числе поверх иконки).

Разделение `background`/`foreground` намеренное: `background` рисуется до дочерних View, `foreground` — после; иначе квадратная иконка перекрывала бы рамку. `android:defaultFocusHighlightEnabled="false"` указан на всех таких элементах, чтобы Android (API 26+) не рисовал поверх системную подсветку.

### AikonIconRegistry — реестр иконок

`core/src/main/java/com/aikon/core/ui/icon/AikonIconRegistry.java`, `AikonIconProvider.java`

Единая точка получения иконки приложения для любого списка.

**Порядок разрешения:**
1. Иконка, явно зарегистрированная под `packageName` (`registerIcon`);
2. Иконка от внешнего `AikonIconProvider` (целый кастомный набор);
3. Системная иконка приложения — запасной вариант.

```java
AikonIconRegistry.registerIcon("com.android.settings", R.drawable.icon_settings_aikon);
AikonIconRegistry.setProvider((context, packageName) -> myIconPack.get(packageName));

Drawable icon = AikonIconRegistry.resolve(context, packageName, packageManager, applicationInfo);
```

Фрагменты не должны вызывать `PackageManager.getApplicationIcon(...)` напрямую — только через `resolve(...)`.

### AikonLoadingOverlay — окно загрузки

`core/src/main/java/com/aikon/core/ui/loading/AikonLoadingOverlay.java`

Небольшое модальное окно «Пожалуйста, подождите…» (`@string/aikon_loading_please_wait`) со стандартным `ProgressBar` в нижней половине экрана. Не пропускает касания.

Привязывается к текущей Activity автоматически через `AikonBaseActivity`. `show()` и `hide()` можно вызывать **из любого потока** — сами переключаются на главный. Внутренний счётчик показов защищает от преждевременного скрытия при вложенных вызовах.

```java
AikonLoadingOverlay.show();
// ... тяжёлая операция ...
AikonLoadingOverlay.hide();

// Обёртка: выполняет задачу в фоновом потоке и гарантированно скрывает окно (даже при исключении)
AikonLoadingOverlay.runAsync(() -> { /* тяжёлая операция */ });
```

**Правило парности:** каждый `show()` должен получить свой `hide()` на любом пути выполнения (в том числе при отмене и закрытии экрана). Фоновые потоки, вызывающие `hide()`, нельзя останавливать через `shutdownNow()` до выполнения этого вызова — иначе окно зависнет на экране.

### AikonContextMenu — контекстное меню

`core/src/main/java/com/aikon/core/ui/menu/AikonContextMenu.java`

Всплывающее меню в левом нижнем углу экрана над панелью кнопок (`PopupWindow`).

```java
AikonContextMenu menu = new AikonContextMenu(context);
menu.addItem("Открыть", this::onOpen);

AikonContextMenu.Item submenu = menu.addSubmenu("Ещё");
submenu.addItem("Подпункт 1", this::doA);
submenu.addItem("Подпункт 2", this::doB);

menu.show(anchorView);
```

Пункт с подпунктами получает стрелку вправо (`@drawable/aikon_arrow_right`). Подменю открывается через **drill-down**: содержимое того же окна заменяется списком дочерних пунктов, сверху добавляется пункт «‹ Назад».

Меню создаётся заново при каждом показе; состояние не переживает `dismiss()`.

### Общие drawable-ресурсы

| Ресурс | Назначение |
|---|---|
| `aikon_popup_bg.xml` | Общий скруглённый фон мелких попапов (окно загрузки, контекстное меню) |
| `aikon_list_item_bg.xml` | Подсветка нажатия элементов списков, меню, ячеек сетки |
| `aikon_list_item_focus_overlay.xml` | Рамка клавиатурного фокуса (через `android:foreground`) |
| `aikon_arrow_right.xml` | Стрелка индикатора подпунктов контекстного меню |
| `aikon_softkey_button_bg.xml` | Подсветка нажатия кнопок нижней панели |

---

## Модуль com.aikon.launcher

### Манифест и точка входа

Обязательные элементы манифеста лаунчера:

```xml
<application
    android:name=".AikonLauncherApplication"   <!-- без него Application.onCreate() НЕ вызывается -->
    android:theme="@style/Theme.Aikon"
    ...>

    <activity android:name=".MainActivity" ...>
        <!-- категории MAIN, HOME, DEFAULT, LAUNCHER -->
    </activity>

    <receiver
        android:name=".LauncherDeviceAdminReceiver"
        android:exported="true"
        android:permission="android.permission.BIND_DEVICE_ADMIN">
        <meta-data android:name="android.app.device_admin"
                   android:resource="@xml/launcher_device_admin"/>
        <intent-filter>
            <action android:name="android.app.action.DEVICE_ADMIN_ENABLED"/>
        </intent-filter>
    </receiver>
</application>
```

`<queries>` содержит записи видимости для сканирования приложений и определения приложений по умолчанию: `MAIN`+`LAUNCHER`, `IMAGE_CAPTURE`, `APP_GALLERY`, `APP_MUSIC`, `VIEW http`, `DELETE`, `DIAL`, `VIEW` контактов, `APP_MESSAGING`. Манифестного приёмника `PACKAGE_*` нет — он не работает (см. ниже).

#### AikonLauncherApplication

`launcher/src/main/java/com/aikon/launcher/AikonLauncherApplication.java`

В `onCreate()`:
1. `AikonScreenLock.setAdminComponent(...)` — передаёт ядру компонент администратора лаунчера;
2. регистрирует **динамический** `PackageChangeReceiver` на `PACKAGE_ADDED/REMOVED/REPLACED` (`registerReceiver` в коде, `RECEIVER_NOT_EXPORTED`). Манифестные приёмники этих событий на Android 8+ не получают, поэтому приёмник живёт столько же, сколько процесс;
3. запускает `AppIndexRepository.ensureLoaded(null)` — прогрев кэша.

#### LauncherDeviceAdminReceiver

Пустой `DeviceAdminReceiver` с политикой `force-lock` (`res/xml/launcher_device_admin.xml`). Принадлежит **только** лаунчеру.

### MainActivity

`launcher/src/main/java/com/aikon/launcher/MainActivity.java`

Наследник `AikonBaseActivity`, единственная Activity лаунчера:

- один экземпляр `AikonSoftkeyBar` на весь жизненный цикл (не пересоздаётся при смене экранов);
- один `FrameLayout`-контейнер для фрагментов;
- синхронизация режима панели с экраном: главный экран (пустой back stack) — `THREE_BUTTON`, любой другой — `TWO_BUTTON` с «назад» = `popBackStack()`;
- `onResume()` выполняет отложенное действие `PendingLaunchHolder` (см. «Очистить кэш и открыть»).

```java
public void openScreen(Fragment fragment, String tag) {
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(tag)
            .commit();
}
```

### Пакет apps — кэш установленных приложений

Полное сканирование `PackageManager.queryIntentActivities(...)` выполняется один раз, дальше кэш поддерживается точечно.

| Класс | Назначение |
|---|---|
| `AppCategoryFlags` | Битовые флаги: `CAMERA`, `GALLERY`, `MUSIC`, `RECORDER`, `GAME` |
| `CachedApp` | Запись кэша: пакет, activity, маска категорий. **Не хранит название** — оно читается из `PackageManager` при отображении |
| `AppIndexStore` | Хранилище кэша в `SharedPreferences` (JSON) |
| `AppIndexRepository` | Главная точка доступа: `getInstance`, `ensureLoaded`, `getAll`, `getByCategory`, `addOrUpdatePackage`, `removePackage` |
| `PackageChangeReceiver` | Приёмник событий установки/удаления/обновления; регистрируется в `AikonLauncherApplication` |
| `AppContextMenuBuilder` | Контекстные меню приложений |
| `DefaultAppResolver` | Определение приложений по умолчанию |
| `PendingLaunchHolder` | Отложенное действие на возврат в лаунчер |

**Как кэш остаётся актуальным:**
1. Пока процесс жив — события `PACKAGE_*` через динамический приёмник обновляют только затронутый пакет.
2. При каждом старте после загрузки кэша с диска выполняется **сверка с системой** (`reconcile`): один запрос списка запускаемых приложений, сравнение с кэшем; расхождения исправляются точечно, а если их больше 8 — полное пересканирование. Это нужно, потому что установки и удаления, произошедшие при неработающем процессе, иначе были бы потеряны навсегда.
3. Лаунчер **исключён** из кэша (фильтр по собственному `packageName` при чтении, сканировании и обновлении) — он не показывается ни в одном списке и не учитывается в счётчике.

Каждая запись соответствует одной запускаемой activity (у пакета их может быть несколько), поэтому `getAll().size()` — число приложений, которые можно запустить.

**Определение игр:** флаг `GAME` ставится, если `ApplicationInfo.category == CATEGORY_GAME` (API 26+) либо выставлен устаревший `FLAG_IS_GAME`.

#### AppContextMenuBuilder

- `showAppManagementMenu` — меню приложения в списках: **Открыть**, **Добавить/Убрать из избранных**, **О приложении** (экран сведений системы), **Очистить кэш и открыть**, **Удалить**.
- `showFavoritesMenu` — меню в избранном: Открыть, Убрать из избранных, О приложении.

Реализация действий:
- **Удалить** — `ACTION_DELETE` с `package:`-URI, системный диалог подтверждения; результат в кэше отследит `PackageChangeReceiver`.
- **Очистить кэш и открыть** — Android не позволяет обычному приложению очистить кэш чужого пакета (требуются системные права), поэтому открывается экран «О приложении», где пользователь нажимает «Очистить кэш» сам; действие открытия откладывается в `PendingLaunchHolder` и выполняется в `MainActivity.onResume()` при возврате в лаунчер.

#### DefaultAppResolver

Определяет приложение по умолчанию заново при каждом клике — пункт меню следует за системным выбором пользователя:

| Метод | Источник |
|---|---|
| `resolvePhone` | `TelecomManager.getDefaultDialerPackage()` (API 23+), запасной путь — обработчик `ACTION_DIAL` |
| `resolveContacts` | обработчик `ACTION_VIEW` на `ContactsContract.Contacts.CONTENT_URI` |
| `resolveSms` | `Telephony.Sms.getDefaultSmsPackage()`, на API 33+ запасной путь — `CATEGORY_APP_MESSAGING` |
| `resolveBrowser` | обработчик `ACTION_VIEW http://` |

Примечание: `RoleManager.getRoleHolders()` недоступен обычным приложениям (`@SystemApi`), поэтому не используется.

### Пакет favorites — избранные приложения

| Класс | Назначение |
|---|---|
| `FavoritesRepository` | Хранилище избранных пакетов (`SharedPreferences` + JSON, порядок добавления сохраняется). При первом запуске заполняется тестовым набором (Настройки/Камера/Калькулятор) |
| `FavoritesFragment` | Полный список избранного; долгое нажатие → `showFavoritesMenu` |

Добавление в избранное — через контекстное меню любого списка приложений. Приложения, которых нет на устройстве, в списке пропускаются без ошибки.

### Пакет home — главный экран

`HomeFragment` — часы (крупный шрифт) и дата (мелкий) по центру сверху, остальное пространство пустое. Обновление — через системный `BroadcastReceiver` на `ACTION_TIME_TICK/TIME_CHANGED/TIMEZONE_CHANGED` без собственных потоков и таймеров. Формат времени берётся из системных настроек (12/24 ч).

### Пакет mainmenu — главное меню

| Класс | Назначение |
|---|---|
| `MainMenuItem` | Модель пункта. Типы: `APP` (пакет), `LIST` (встроенный список), `DEFAULT_APP` (приложение по умолчанию), `LINK` (особый вызов) |
| `MainMenuData` | Статичный список из 9 пунктов |
| `MainMenuAdapter` | Адаптер сетки |
| `SquareFrameLayout` | `FrameLayout`, принудительно квадратный (высота = ширине) |
| `MainMenuFragment` | Экран меню: `GridLayoutManager`, 3 колонки; фокус при открытии — на пункте «Приложения» (позиция 4) |

Состав меню:

| # | Пункт | Тип | Действие |
|---|---|---|---|
| 1 | Телефон | DEFAULT_APP | `DefaultAppResolver.resolvePhone` |
| 2 | Контакты | DEFAULT_APP | `resolveContacts` |
| 3 | SMS | DEFAULT_APP | `resolveSms` |
| 4 | Игры | LIST | `GamesListFragment` |
| 5 | Приложения | LIST | `AppsQuickListFragment` |
| 6 | Мультимедиа | LIST | `MultimediaListFragment` |
| 7 | Магазин | LINK | F-Droid (`org.fdroid.fdroid`); если не установлен — открывается `f-droid.org` в браузере |
| 8 | Настройки | APP | `com.android.settings` |
| 9 | Браузер | DEFAULT_APP | `resolveBrowser` |

Если приложение для пункта не найдено — `Toast` (`@string/app_not_found`).

### Пакет applist — списки приложений

| Класс | Назначение |
|---|---|
| `AppListFragment` | Полный список всех приложений (из кэша), иконки через `AikonIconRegistry`; долгое нажатие → `showAppManagementMenu` |
| `AppsQuickListFragment` | Список пункта «Приложения»: «Все приложения» (`FULL`, подпись — число запускаемых приложений) + заданные приложения (калькулятор, календарь, заметки). Приложения ищутся по системной категории, затем перебором пакетов популярных прошивок. Контекстное меню не подключено |

### Пакет games — список игр

`GamesListFragment` — установленные приложения с флагом `GAME` из кэша, без обращений к `PackageManager` для определения категории. Долгое нажатие → `showAppManagementMenu`.

Интеграция с виджетами/ярлыками J2ME Loader была реализована и **удалена**: закрепление ярлыков в лаунчере не заработало, дальнейшая диагностика отложена.

### Пакет multimedia — список мультимедиа

`MultimediaListFragment` — приложения с флагами `CAMERA | GALLERY | MUSIC | RECORDER` из кэша; системных запросов на этом экране нет, только фильтрация. Долгое нажатие → `showAppManagementMenu`.

---

## Модуль com.aikon.filemanager

Отдельное приложение (`com.android.application`, `namespace`/`applicationId` = `com.aikon.filemanager`), подключает `core` через `implementation project(':core')`.

### Манифест

- разрешения: `READ/WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=29`), `MANAGE_EXTERNAL_STORAGE` (Android 11+), `REQUEST_INSTALL_PACKAGES` (открытие `.apk`);
- `FileProvider` (`${applicationId}.fileprovider`, `res/xml/file_paths.xml` с `<root-path>`) — открытие и отправка файлов;
- `configChanges` на `MainActivity` — поворот не пересоздаёт Activity (иначе терялось бы состояние копирования/перемещения).

### Структура

| Класс | Назначение |
|---|---|
| `MainActivity` | Контейнер страниц, панель из двух кнопок, навигация, состояние копирования/перемещения (`TransferState`), проверка доступа к хранилищу |
| `DirectoryFragment` | Одна страница = одна папка: загрузка списка, клики, контекстные меню, диалоги |
| `FileOperations` | Блокирующие операции: копирование, перемещение, удаление, подсчёт размера папки, время создания, уникальное имя |
| `FileIntents` | Открытие (`ACTION_VIEW`, обычное и с принудительным выбором приложения) и отправка (`ACTION_SEND`) через `FileProvider` |
| `FileIconResolver` | Единая точка выбора иконки файла (тестовые векторные иконки; для `.apk` — иконка из самого файла) |
| `StorageAccess` | Проверка и запрос доступа к файлам (Android 11+: «все файлы»; ниже — runtime-разрешения) |
| `FileIo` | Общий фоновый поток чтения каталогов |

### Поведение

- Стартовая папка — корень `Environment.getExternalStorageDirectory()`. Каждая открытая папка — новая страница в back stack; «Назад» возвращает на предыдущую, на первой странице закрывает приложение.
- Первый пункт каждой страницы — «На уровень выше» (переход в родительскую папку, в том числе выше `sdcard`; системные каталоги Android может не отдать для чтения — тогда показывается сообщение).
- Полный список: имя сверху, размер/«Папка» и дата снизу, иконка справа. Папки отсортированы раньше файлов, внутри групп — по имени без учёта регистра. Дата создания: если файловая система её не отдаёт — показывается дата изменения.
- Окно загрузки показывается при открытии каждой страницы, а также при копировании, перемещении, удалении и подсчёте размера папки.

### Контекстное меню

Открывается долгим нажатием, левой кнопкой панели и физической клавишей MENU. Кнопка/клавиша открывает меню выделенного элемента; если выделения нет или выделен пункт «На уровень выше» — меню текущей папки («Создать папку», «Свойства»).

Пункты элемента: **Открыть**, **Открыть с помощью** (только файлы, принудительный выбор приложения), **Копировать в…**, **Переместить**, **Отправить** (только файлы), **Создать папку**, **Свойства**, **Удалить**.

### Копирование и перемещение

После выбора «Копировать в…»/«Переместить» все страницы показываются **простым списком только из папок**, долгое нажатие у элементов отключено. Меню (кнопка или MENU) содержит «Сюда» и «Отмена». После операции или отмены приложение возвращает пользователя на страницу, откуда он начал. Если имя занято — копия получает имя вида `файл (1).txt`; перемещение в ту же папку ничего не делает; папку нельзя поместить в саму себя. Перемещение сначала пробует быстрый `rename`, при неудаче (другой том) копирует и удаляет оригинал.

### Диалоги

Подтверждение удаления, ввод имени папки и «Свойства» пока используют системный `AlertDialog` (`DirectoryFragment`: `confirmDelete`, `promptCreateFolder`, `showPropertiesDialog`). Замена на окна из `core` запланирована.

---

## Как сделать новое приложение проекта

1. Создать модуль **Android Application** (не Library и не Dynamic Feature), подключить `implementation project(':core')`.
2. Указать `android:theme="@style/Theme.Aikon"` (или наследника) в манифесте.
3. Наследовать все Activity от `AikonBaseActivity`.
4. После `findViewById` панели вызвать `attachSoftkeyBar(bar)` — иначе физические клавиши не заработают.
5. Для списков использовать `AikonListView` + `AikonListAdapter` + `AikonListItem`; иконки приложений — через `AikonIconRegistry.resolve(...)`.
6. Долгие операции выполнять в фоновом потоке и оборачивать в `AikonLoadingOverlay.show()`/`hide()` или `runAsync(...)`; соблюдать правило парности.
7. Контекстное меню подключать только там, где оно осмысленно: через третий параметр конструктора `AikonListAdapter`. У настроек и простых списков меню нет — просто не передавайте `OnItemLongClickListener`.
8. Не объявлять в приложении то, что не нужно: права администратора, широкие разрешения. Компонент администратора нужен только приложению, которому требуется блокировка экрана; остальным ничего делать не нужно — панель без заданного компонента просто не блокирует.
9. Если приложению нужно видеть другие пакеты — добавить `<queries>` (Android 11+).
10. Предпочитать системные категории и action'ы (`CATEGORY_APP_*`, приложения по умолчанию) вместо жёстко заданных имён пакетов.
11. Не помещать в манифест `core` компоненты, нужные одному приложению: манифест ядра сливается со всеми приложениями.

---

## Типичные ошибки настройки

| Симптом | Причина |
|---|---|
| `This application is not configured to use dynamic features` | Модуль создан как Dynamic Feature; нужен Android Library/Application |
| `Library projects cannot set applicationId` | В `core/build.gradle` остался `applicationId`; в библиотеке используется только `namespace` |
| `Cannot resolve symbol '@+id/...'` в ядре | Падает синхронизация Gradle либо не задан `namespace` |
| Фрагмент не отображается | В `MainActivity` отсутствует транзакция `replace(...)` либо layout не содержит `fragment_container` |
| Блокировка не срабатывает, «Компонент не задан» | В манифесте нет `android:name=".AikonLauncherApplication"`, и `Application.onCreate()` не вызывается |
| Список приложений не обновляется после установки | Приёмник `PACKAGE_*` объявлен в манифесте (на Android 8+ не работает) — регистрировать нужно в коде |
| Окно загрузки «зависло» | `show()` без парного `hide()` на каком-то пути выполнения (в т.ч. задача отменена через `shutdownNow()`) |
| Рамка фокуса перекрыта иконкой | Рамка задана в `background`, а не в `foreground` |

---

## Известные ограничения и TODO

- **AIKON Themes** (`com.aikon.themes`) не реализован; схема описана в разделе выше. Требует перевода части оформления с XML-атрибутов на Java-слой и проверки подписи источника темы.
- **Системный интерфейс** (приложение-заглушка с ядром и подхватом темы) не реализован.
- `AikonScreenLock.DEBUG_TOASTS = true` — отладочные Toast'ы нужно выключить перед выпуском.
- Блокировка экрана работает только через права администратора устройства; вариант через службу специальных возможностей не реализован.
- Очистка кэша чужого приложения невозможна без системных прав: «Очистить кэш и открыть» требует ручного нажатия пользователем.
- `AikonIconRegistry` пока без собственного набора иконок; файловый менеджер использует тестовые векторные иконки (`FileIconResolver`).
- Диалоги подтверждения, ввода текста и «Свойства» в файловом менеджере — системные, замена на окна из `core` запланирована.
- Счётчик приложений (`all_apps_count_format`) использует `%d` без русских форм множественного числа — потребуется `<plurals>`.
- Обновление пакета, изменившее его категорию за время неработающего процесса лаунчера, подхватится только при следующем событии для этого пакета.
- Интеграция с ярлыками/виджетами J2ME Loader удалена; причина неудачи не установлена.

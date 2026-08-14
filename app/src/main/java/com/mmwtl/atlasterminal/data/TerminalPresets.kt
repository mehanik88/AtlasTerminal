package com.mmwtl.atlasterminal.data

data class PresetCategory(
    val id: String,
    val title: String,
    val items: List<PresetItem>
)

data class PresetItem(
    val id: String,
    val title: String,
    val command: String,
    val description: String = "",
    val category: String = "",
    val isCustom: Boolean = false
)

object TerminalPresets {
    val BUILT_IN_CATEGORIES = listOf(
        PresetCategory(
            id = "system",
            title = "Система и железо",
            items = listOf(
                PresetItem("sys_props", "Все параметры getprop", "getprop", "Выводит все системные свойства Android"),
                PresetItem("sys_model", "Модель и платформа", "echo \"Model: \$(getprop ro.product.model)\"; echo \"Platform: \$(getprop ro.board.platform)\"; echo \"Android: \$(getprop ro.build.version.release)\"", "Краткая информация об устройстве и версии ОС"),
                PresetItem("sys_df", "Дисковое пространство (df -h)", "df -h", "Свободное место на разделах /system, /data, /storage"),
                PresetItem("sys_free", "Память ОЗУ (free -m)", "free -m", "Использование оперативной памяти"),
                PresetItem("sys_uptime", "Время работы (uptime)", "uptime", "Время с момента запуска системы и средняя загрузка"),
                PresetItem("sys_uname", "Ядро Linux (uname -a)", "uname -a", "Версия ядра Linux и архитектура процессора"),
                PresetItem("sys_top", "Топ процессов (top)", "top -n 1 -m 10", "10 наиболее ресурсоемких процессов прямо сейчас")
            )
        ),
        PresetCategory(
            id = "apps",
            title = "Приложения (PM / AM)",
            items = listOf(
                PresetItem("pm_list_3", "Сторонние приложения", "pm list packages -3", "Список установленных пользователем APK"),
                PresetItem("pm_list_e", "Включенные пакеты", "pm list packages -e", "Все активные пакеты в системе"),
                PresetItem("pm_list_d", "Отключенные пакеты", "pm list packages -d", "Пакеты, находящиеся в состоянии disabled"),
                PresetItem("pm_path_settings", "Путь к Settings.apk", "pm path com.android.settings", "Определение расположения APK настроек"),
                PresetItem("am_kill_bg", "Остановить фоновые процессы", "am kill-all", "Безопасная очистка фоновых приложений"),
                PresetItem("pm_enable_install", "Разрешить сторонние APK", "settings put global install_non_market_apps 1", "Включение разрешения установки из неизвестных источников")
            )
        ),
        PresetCategory(
            id = "media",
            title = "Аудио и медиакодеки",
            items = listOf(
                PresetItem("media_codec", "Дамп кодеков (media.codec)", "dumpsys media.codec", "Информация обо всех зарегистрированных декодерах и энкодерах C2/OMX"),
                PresetItem("media_audio", "Дамп аудиоподсистемы", "dumpsys audio", "Текущие громкости, аудиопотоки, устройства вывода"),
                PresetItem("media_flinger", "Дамп AudioFlinger", "dumpsys media.audio_flinger", "Детальное состояние микшеров и треков AudioFlinger"),
                PresetItem("media_session", "Медиа-сессии", "dumpsys media_session", "Текущие активные медиа-плееры и фоновые воспроизведения")
            )
        ),
        PresetCategory(
            id = "display",
            title = "Экран и окна",
            items = listOf(
                PresetItem("wm_focus", "Текущее активное окно", "dumpsys window | grep -E \"mCurrentFocus|mFocusedApp\"", "Показывает пакет и Activity окна на переднем плане"),
                PresetItem("wm_size", "Разрешение экрана (wm size)", "wm size", "Текущее физическое и переопределенное разрешение экрана ГУ"),
                PresetItem("wm_density", "Плотность экрана (wm density)", "wm density", "Текущее DPI экрана"),
                PresetItem("am_top_act", "Верхняя Activity в стеке", "dumpsys activity activities | grep -E \"Hist|topResumedActivity\" | head -n 10", "Информация о стеке запущенных активностей")
            )
        ),
        PresetCategory(
            id = "network",
            title = "Сеть и логи",
            items = listOf(
                PresetItem("net_ip", "Сетевые интерфейсы (ip a)", "ip addr show", "Список IP-адресов Wi-Fi, Ethernet, USB"),
                PresetItem("net_ports", "Открытые порты (netstat)", "netstat -tuln 2>/dev/null || cat /proc/net/tcp", "Слушающие TCP/UDP порты на ГУ"),
                PresetItem("logcat_tail", "Последние 100 строк logcat", "logcat -d -t 100", "Свежий срез системного журнала Android"),
                PresetItem("logcat_errors", "Только ошибки logcat (*:E)", "logcat -d *:E -t 100", "Вывод ошибок из журнала за последнее время"),
                PresetItem("logcat_clear", "Очистить буфер logcat", "logcat -c", "Сброс логов в памяти"),
                PresetItem("usb_config", "Текущий режим USB", "getprop sys.usb.config", "Конфигурация ADB / MTP / Accessory USB порта")
            )
        ),
        PresetCategory(
            id = "root",
            title = "Root",
            items = listOf(
                PresetItem("mount_list", "Список смонтированных разделов", "mount | grep -E \"/system|/vendor|/data|/storage\"", "Файловые системы и флаги монтирования"),
                PresetItem("su_whoami", "Проверка root (id / whoami)", "id; whoami", "Проверка текущего UID/GID и прав пользователя"),
                PresetItem("soft_reboot", "Мягкий перезапуск (restart zygote)", "setprop ctl.restart zygote", "Перезапуск Android Framework без перезагрузки ядра")
            )
        )
    )

    fun getAllPresets(customPresets: List<CustomPreset>): List<PresetItem> {
        val builtIn = BUILT_IN_CATEGORIES.flatMap { cat ->
            cat.items.map { it.copy(category = cat.title) }
        }
        val custom = customPresets.map {
            PresetItem(
                id = it.id,
                title = it.title,
                command = it.command,
                description = it.description,
                category = it.category.ifBlank { "Мои команды" },
                isCustom = true
            )
        }
        return builtIn + custom
    }
}

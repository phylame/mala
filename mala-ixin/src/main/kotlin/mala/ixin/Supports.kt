package mala.ixin

import jclp.io.IOUtils
import jclp.log.Log
import jclp.text.Converter
import jclp.text.ConverterManager
import jclp.util.StringUtils
import mala.core.ResourceManager
import java.awt.*
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import kotlin.collections.HashMap

infix fun Int.x(height: Int) = Dimension(this, height)

operator fun Dimension.minus(scale: Double) = Dimension((width * scale).toInt(), (height * scale).toInt())

val MouseEvent.isLeft: Boolean get() = SwingUtilities.isLeftMouseButton(this)

val MouseEvent.isRight: Boolean get() = SwingUtilities.isRightMouseButton(this)

fun ResourceManager.iconFor(name: String): Icon? {
    return ImageIcon(resourceFor(name) ?: return null)
}

fun ResourceManager.imageFor(name: String): Image? {
    return Toolkit.getDefaultToolkit().getImage(resourceFor(name) ?: return null)
}

object IxIn {
    private const val TAG = "IxIn"

    const val JAVA_THEME = "Java"
    const val SYSTEM_THEME = "System"
    const val DEFAULT_THEME = "Default"
    const val MNEMONIC_PREFIX = '&'

    var isAntiAliasing: Boolean = false
        set(value) {
            updateAntiAliasing(value)
            field = value
        }

    fun updateAntiAliasing(enable: Boolean) {
        System.setProperty("awt.useSystemAAFontSettings", if (enable) "on" else "off")
        System.setProperty("swing.aatext", enable.toString())
    }

    var isWindowDecorated: Boolean = false
        set(value) {
            updateWindowDecorated(value)
            field = value
        }

    fun updateWindowDecorated(enable: Boolean) {
        JDialog.setDefaultLookAndFeelDecorated(enable)
        JFrame.setDefaultLookAndFeelDecorated(enable)
    }

    var iconSet = System.getProperty("ixin.icons").orEmpty()

    var swingTheme: String = System.getProperty("ixin.theme").orEmpty()
        set(value) {
            updateSwingTheme(value)
            field = value
        }

    val themes = HashMap<String, String>()

    fun getLafName(name: String): String = themes[name] ?: when (name) {
        DEFAULT_THEME -> UIManager.getLookAndFeel().javaClass.name
        SYSTEM_THEME -> UIManager.getSystemLookAndFeelClassName()
        JAVA_THEME -> UIManager.getCrossPlatformLookAndFeelClassName()
        else -> name
    }

    fun updateSwingTheme(name: String) {
        if (name.isNotEmpty()) {
            try {
                UIManager.setLookAndFeel(getLafName(name))
            } catch (e: Exception) {
                throw RuntimeException("Cannot set to new laf: $name", e)
            }
        } else {
            Log.d(TAG, "empty laf theme specified")
        }
    }

    var globalFont: Font = Font.decode(System.getProperty("ixin.font"))
        set(value) {
            updateGlobalFont(value)
            field = value
        }

    private val fontKeys by lazy {
        val keys = LinkedList<String>()
        IOUtils.openResource("!mala/ixin/font-keys.lst", IxIn::class.java.classLoader)?.bufferedReader()?.forEachLine {
            keys.add(it.trim())
        }
        keys
    }

    fun updateGlobalFont(font: Font) {
        val defaults = UIManager.getLookAndFeelDefaults()
        for (key in fontKeys) {
            val value = defaults[key]
            defaults[key] = when (value) {
                null -> font
                is Font -> font.deriveFont(value.style)
                is UIDefaults.ActiveValue -> font.deriveFont((value.createValue(defaults) as Font).style)
                is UIDefaults.LazyValue -> font.deriveFont((value.createValue(defaults) as Font).style)
                else -> throw RuntimeException("unknown name for key $key")
            }
        }
    }

    val isMnemonicSupport by lazy {
        "mac" !in System.getProperty("os.name")
    }

    var isMnemonicEnable = isMnemonicSupport

    data class MnemonicResults(val name: String, val mnemonic: Int, val index: Int) {
        val isEnable get() = isMnemonicEnable && mnemonic != 0
    }

    fun getMnemonic(name: String): MnemonicResults {
        val text = StringBuilder()
        var mnemonic = 0
        var index = -1

        var foundPrefix = false
        for ((i, ch) in name.withIndex()) {
            if (foundPrefix) {
                if (ch.isLetterOrDigit()) {
                    mnemonic = ch.toInt()
                    index = i - 1
                } else if (ch != MNEMONIC_PREFIX) {
                    text.append(MNEMONIC_PREFIX)
                }
                text.append(ch)
                foundPrefix = false
            } else {
                if (ch == MNEMONIC_PREFIX) {
                    foundPrefix = true
                } else {
                    text.append(ch)
                }
            }
        }

        return MnemonicResults(text.toString(), mnemonic, index)
    }

    init {
        for (feel in UIManager.getInstalledLookAndFeels()) {
            themes[feel.name] = feel.className
        }

        ConverterManager.registerConverter(Point::class.java, object : Converter<Point> {
            override fun render(p: Point): String = "${p.x}-${p.y}"

            override fun parse(str: String): Point {
                val pair = StringUtils.partition(str, "-")
                return Point(Integer.decode(pair.first.trim()), Integer.decode(pair.second.trim()))
            }
        })

        ConverterManager.registerConverter(Dimension::class.java, object : Converter<Dimension> {
            override fun render(d: Dimension): String = "${d.width}-${d.height}"

            override fun parse(str: String): Dimension {
                val pair = StringUtils.partition(str, "-")
                return Dimension(Integer.decode(pair.first.trim()), Integer.decode(pair.second.trim()))
            }
        })

        ConverterManager.registerConverter(Font::class.java, object : Converter<Font> {
            override fun parse(str: String): Font = Font.decode(str)

            override fun render(f: Font): String {
                val b = StringBuilder(f.family).append('-')
                when (f.style) {
                    Font.PLAIN -> b.append("plain")
                    Font.BOLD -> b.append("bold")
                    Font.ITALIC -> b.append("italic")
                    Font.BOLD or Font.ITALIC -> b.append("bolditalic")
                }
                return b.append('-').append(f.size).toString()
            }
        })

        ConverterManager.registerConverter(Color::class.java, object : Converter<Color> {
            override fun render(c: Color): String = "#%X".format(c.rgb).substring(2)

            override fun parse(str: String): Color = Color.decode(str)
        })
    }
}

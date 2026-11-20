# Генерирует модели, блокстейты, крафты, лут-таблицы и чистые 16x16 текстуры.
# Текстуры без чёрных контуров: базовый цвет + светлый блик + мягкий цветной оттенок.
# Запуск: powershell -ExecutionPolicy Bypass -File tools\generate_assets.ps1
Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\src\main\resources'
$assets = Join-Path $root 'assets\herbcraft'
$data = Join-Path $root 'data\herbcraft'

$types = @(
    @{ id='green';   hex='63B84A' },
    @{ id='golden';  hex='E8C63F' },
    @{ id='blue';    hex='4E9BE8' },
    @{ id='purple';  hex='A75AC9' },
    @{ id='rainbow'; hex='FF6FB1' }
)

$utf8 = New-Object System.Text.UTF8Encoding($false)

function Write-Text([string]$path, [string]$text) {
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, $text, $utf8)
}

function New-Texture([string]$path, [scriptblock]$draw) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    & $draw $g
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

# Смешивание канала с белым (блик) или чёрным (мягкий оттенок, не контур).
function Mix([int]$c, [int]$target, [double]$w) { [int]($c + ($target - $c) * $w) }

foreach ($t in $types) {
    $id = $t.id
    $hex = $t.hex
    $r = [Convert]::ToInt32($hex.Substring(0,2),16)
    $g8 = [Convert]::ToInt32($hex.Substring(2,2),16)
    $b = [Convert]::ToInt32($hex.Substring(4,2),16)

    $col   = [System.Drawing.Color]::FromArgb($r, $g8, $b)
    $light = [System.Drawing.Color]::FromArgb((Mix $r 255 0.45), (Mix $g8 255 0.45), (Mix $b 255 0.45))
    $shade = [System.Drawing.Color]::FromArgb((Mix $r 0 0.22), (Mix $g8 0 0.22), (Mix $b 0 0.22))

    $brush = New-Object System.Drawing.SolidBrush $col
    $lightBrush = New-Object System.Drawing.SolidBrush $light
    $shadeBrush = New-Object System.Drawing.SolidBrush $shade

    $stemBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(78,150,60))
    $stemLight = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(112,188,86))
    $whiteBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(250,250,246))
    $paperBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(224,207,172))
    $glassBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(148,170,186))
    $glassLight = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(208,224,234))
    $metalBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(202,209,218))
    $metalLight = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(236,240,244))

    # --- Высушенная трава (семена): рыхлый пучок листьев со светлыми бликами ---
    New-Texture (Join-Path $assets "textures\item\${id}_herb.png") {
        param($g)
        $g.FillEllipse($shadeBrush, 4, 4, 7, 5)
        $g.FillEllipse($brush, 3, 3, 7, 5)
        $g.FillEllipse($brush, 7, 2, 6, 6)
        $g.FillEllipse($brush, 4, 7, 6, 5)
        $g.FillEllipse($lightBrush, 5, 3, 3, 2)
        $g.FillEllipse($lightBrush, 8, 3, 2, 2)
        $g.FillRectangle($stemBrush, 7, 9, 2, 5)
    }

    # --- Скрутка: бумага с lighter-полоской, цветной кончик, тлеющий уголёк ---
    New-Texture (Join-Path $assets "textures\item\${id}_joint.png") {
        param($g)
        $g.FillRectangle($paperBrush, 3, 6, 10, 4)
        $g.FillRectangle($whiteBrush, 3, 6, 10, 1)
        $g.FillRectangle($brush, 11, 6, 3, 4)
        $g.FillRectangle($lightBrush, 12, 6, 1, 4)
        $g.FillRectangle([System.Drawing.Brushes]::Orange, 2, 7, 1, 2)
        $g.FillRectangle([System.Drawing.Brushes]::Yellow, 2, 7, 1, 1)
    }

    # --- Жидкость: стеклянный флакон с цветной жидкостью и бликом ---
    New-Texture (Join-Path $assets "textures\item\${id}_liquid.png") {
        param($g)
        $g.FillRectangle($metalBrush, 6, 1, 4, 2)
        $g.FillRectangle($metalLight, 6, 1, 4, 1)
        $g.FillRectangle($glassBrush, 6, 3, 4, 2)
        $g.FillRectangle($glassBrush, 4, 5, 8, 10)
        $g.FillRectangle($glassLight, 4, 5, 2, 10)
        $g.FillRectangle($brush, 5, 8, 6, 6)
        $g.FillRectangle($lightBrush, 5, 8, 6, 1)
        $g.FillRectangle($whiteBrush, 10, 6, 1, 4)
    }

    # --- Вэйп: металлический корпус, цветное окошко, светодиод и кнопка ---
    New-Texture (Join-Path $assets "textures\item\${id}_ecig.png") {
        param($g)
        $g.FillRectangle($whiteBrush, 6, 0, 4, 3)
        $g.FillRectangle($metalBrush, 5, 3, 6, 11)
        $g.FillRectangle($metalLight, 5, 3, 2, 11)
        $g.FillRectangle($brush, 6, 5, 4, 5)
        $g.FillRectangle($lightBrush, 6, 5, 4, 1)
        $g.FillRectangle([System.Drawing.Brushes]::Lime, 7, 2, 2, 1)
        $g.FillRectangle($glassBrush, 7, 11, 2, 2)
    }

    # --- Дикий куст: зелёный стебель и яркие листья с бликами ---
    New-Texture (Join-Path $assets "textures\block\${id}_wild.png") {
        param($g)
        $g.FillRectangle($stemBrush, 7, 6, 2, 10)
        $g.FillRectangle($stemLight, 7, 6, 1, 10)
        $g.FillEllipse($shadeBrush, 3, 3, 7, 6)
        $g.FillEllipse($brush, 2, 3, 7, 6)
        $g.FillEllipse($brush, 7, 2, 6, 6)
        $g.FillEllipse($brush, 2, 8, 6, 5)
        $g.FillEllipse($lightBrush, 4, 4, 3, 2)
        $g.FillEllipse($lightBrush, 8, 3, 2, 2)
    }

    # --- Четыре стадии роста: стебель + листья, блик на старших стадиях ---
    for ($stage = 0; $stage -le 3; $stage++) {
        $h = 3 + $stage * 3
        $top = [math]::Max(3, 15 - $h)
        New-Texture (Join-Path $assets "textures\block\${id}_crop_${stage}.png") {
            param($gg)
            $gg.FillRectangle($stemBrush, 7, $top, 2, $h)
            $gg.FillRectangle($stemLight, 7, $top, 1, $h)
            $gg.FillEllipse($shadeBrush, 5, [math]::Max(2,13-$h), 6, 4)
            $gg.FillEllipse($brush, 4, [math]::Max(2,13-$h), 6, 4)
            if ($stage -ge 1) { $gg.FillEllipse($brush, 8, [math]::Max(1,11-$h), 5, 4) }
            if ($stage -ge 2) { $gg.FillEllipse($brush, 2, [math]::Max(4,12-$h), 5, 4) }
            if ($stage -ge 3) {
                $gg.FillEllipse($brush, 9, [math]::Max(5,14-$h), 4, 3)
                $gg.FillEllipse($lightBrush, 5, [math]::Max(2,13-$h), 2, 1)
            }
        }.GetNewClosure()
    }

    $brush.Dispose(); $lightBrush.Dispose(); $shadeBrush.Dispose()
    $stemBrush.Dispose(); $stemLight.Dispose(); $whiteBrush.Dispose(); $paperBrush.Dispose()
    $glassBrush.Dispose(); $glassLight.Dispose(); $metalBrush.Dispose(); $metalLight.Dispose()

    # --- Модели предметов ---
    foreach ($what in @('herb','joint','liquid','ecig')) {
        $model = @{ parent='minecraft:item/generated'; textures=@{ layer0="herbcraft:item/${id}_${what}" } } | ConvertTo-Json -Depth 5 -Compress
        Write-Text (Join-Path $assets "models\item\${id}_${what}.json") $model
    }
    $wildItemModel = @{ parent='minecraft:item/generated'; textures=@{ layer0="herbcraft:block/${id}_wild" } } | ConvertTo-Json -Depth 5 -Compress
    Write-Text (Join-Path $assets "models\item\${id}_wild.json") $wildItemModel

    # --- Модели блоков ---
    $wildModel = @{ parent='minecraft:block/cross'; textures=@{ cross="herbcraft:block/${id}_wild" } } | ConvertTo-Json -Depth 5 -Compress
    Write-Text (Join-Path $assets "models\block\${id}_wild.json") $wildModel

    for ($age = 0; $age -le 7; $age++) {
        $stage = [math]::Floor($age / 2)
        $cropModel = @{ parent='minecraft:block/crop'; textures=@{ crop="herbcraft:block/${id}_crop_${stage}" } } | ConvertTo-Json -Depth 5 -Compress
        Write-Text (Join-Path $assets "models\block\${id}_crop_${age}.json") $cropModel
    }

    # --- Блокстейты ---
    $wildBs = @{ variants=@{ ''=@{ model="herbcraft:block/${id}_wild" } } } | ConvertTo-Json -Depth 6 -Compress
    Write-Text (Join-Path $assets "blockstates\${id}_wild.json") $wildBs

    $variants = @{}
    for ($age = 0; $age -le 7; $age++) {
        $variants["age=$age"] = @{ model="herbcraft:block/${id}_crop_${age}" }
    }
    $cropBs = @{ variants=$variants } | ConvertTo-Json -Depth 6 -Compress
    Write-Text (Join-Path $assets "blockstates\${id}_crop.json") $cropBs

    # --- Крафты ---
    $jointRecipe = @{
        type='minecraft:crafting_shaped'
        pattern=@('HPH')
        key=@{ H=@{ item="herbcraft:${id}_herb" }; P=@{ item='minecraft:paper' } }
        result=@{ item="herbcraft:${id}_joint"; count=1 }
    } | ConvertTo-Json -Depth 8 -Compress
    Write-Text (Join-Path $data "recipes\${id}_joint.json") $jointRecipe

    $ecigRecipe = @{
        type='minecraft:crafting_shaped'
        pattern=@('IG','RL')
        key=@{
            I=@{ item='minecraft:iron_ingot' }
            G=@{ item='minecraft:glass_pane' }
            R=@{ item='minecraft:redstone' }
            L=@{ item="herbcraft:${id}_liquid" }
        }
        result=@{ item="herbcraft:${id}_ecig"; count=1 }
    } | ConvertTo-Json -Depth 8 -Compress
    Write-Text (Join-Path $data "recipes\${id}_ecig.json") $ecigRecipe

    # --- Лут-таблицы ---
    $wildLoot = @{
        type='minecraft:block'
        pools=@(@{
            rolls=1
            entries=@(@{ type='minecraft:item'; name="herbcraft:${id}_herb" })
            conditions=@(@{ condition='minecraft:survives_explosion' })
        })
    } | ConvertTo-Json -Depth 10 -Compress
    Write-Text (Join-Path $data "loot_tables\blocks\${id}_wild.json") $wildLoot

    $cropLoot = @{
        type='minecraft:block'
        pools=@(
            @{
                rolls=1
                entries=@(@{ type='minecraft:item'; name="herbcraft:${id}_herb"; functions=@(@{ function='minecraft:set_count'; count=2 }) })
                conditions=@(@{ condition='minecraft:block_state_property'; block="herbcraft:${id}_crop"; properties=@{ age='7' } })
            },
            @{
                rolls=1
                entries=@(@{ type='minecraft:item'; name="herbcraft:${id}_herb" })
            }
        )
    } | ConvertTo-Json -Depth 12 -Compress
    Write-Text (Join-Path $data "loot_tables\blocks\${id}_crop.json") $cropLoot
}

Write-Output "Assets generated for $($types.Count) herb varieties."

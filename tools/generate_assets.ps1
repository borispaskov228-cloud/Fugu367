# Генерирует модели, блокстейты, крафты, лут-таблицы и простые 16x16 текстуры.
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

foreach ($t in $types) {
    $id = $t.id
    $hex = $t.hex
    $r = [Convert]::ToInt32($hex.Substring(0,2),16)
    $g8 = [Convert]::ToInt32($hex.Substring(2,2),16)
    $b = [Convert]::ToInt32($hex.Substring(4,2),16)
    $col = [System.Drawing.Color]::FromArgb($r,$g8,$b)
    $dark = [System.Drawing.Color]::FromArgb([math]::Max(0,$r-60),[math]::Max(0,$g8-60),[math]::Max(0,$b-60))
    $brush = New-Object System.Drawing.SolidBrush $col
    $darkBrush = New-Object System.Drawing.SolidBrush $dark
    $greenBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(70,140,55))
    $whiteBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(245,240,220))
    $grayBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(70,70,78))
    $paperBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(210,190,150))

    # --- Текстура: высушенная трава (семена) ---
    New-Texture (Join-Path $assets "textures\item\${id}_herb.png") {
        param($g)
        $g.FillEllipse($brush, 4, 3, 7, 5)
        $g.FillEllipse($brush, 7, 2, 6, 5)
        $g.FillEllipse($brush, 3, 7, 6, 5)
        $g.FillEllipse($darkBrush, 8, 8, 5, 4)
        $g.FillRectangle($greenBrush, 7, 9, 2, 6)
    }

    # --- Текстура: скрутка ---
    New-Texture (Join-Path $assets "textures\item\${id}_joint.png") {
        param($g)
        $g.FillRectangle($paperBrush, 3, 6, 11, 4)
        $g.FillRectangle($brush, 11, 6, 3, 4)
        $g.FillRectangle($darkBrush, 3, 6, 2, 4)
        $g.FillRectangle([System.Drawing.Brushes]::Orange, 2, 7, 1, 2)
    }

    # --- Текстура: жидкость ---
    New-Texture (Join-Path $assets "textures\item\${id}_liquid.png") {
        param($g)
        $g.FillRectangle($grayBrush, 6, 1, 4, 2)
        $g.FillRectangle($grayBrush, 5, 3, 6, 12)
        $g.FillRectangle($brush, 6, 6, 4, 8)
        $g.FillRectangle($darkBrush, 6, 3, 4, 3)
    }

    # --- Текстура: вэйп ---
    New-Texture (Join-Path $assets "textures\item\${id}_ecig.png") {
        param($g)
        $g.FillRectangle($grayBrush, 6, 1, 4, 2)
        $g.FillRectangle($grayBrush, 5, 3, 6, 11)
        $g.FillRectangle($brush, 6, 5, 4, 5)
        $g.FillRectangle($whiteBrush, 6, 13, 4, 2)
        $g.FillRectangle([System.Drawing.Brushes]::Lime, 7, 2, 2, 1)
    }

    # --- Текстура: дикий куст ---
    New-Texture (Join-Path $assets "textures\block\${id}_wild.png") {
        param($g)
        $g.FillRectangle($greenBrush, 7, 7, 2, 9)
        $g.FillEllipse($brush, 3, 3, 7, 6)
        $g.FillEllipse($brush, 7, 2, 6, 6)
        $g.FillEllipse($darkBrush, 2, 8, 6, 5)
        $g.FillEllipse($darkBrush, 9, 8, 5, 5)
    }

    # --- Текстуры: 4 стадии роста культуры ---
    for ($stage = 0; $stage -le 3; $stage++) {
        $h = 3 + $stage * 3
        New-Texture (Join-Path $assets "textures\block\${id}_crop_${stage}.png") {
            param($gg)
            $gg.FillRectangle($greenBrush, 7, [math]::Max(3,15-$h), 2, $h)
            $gg.FillEllipse($brush, 5, [math]::Max(2,13-$h), 6, 4)
            if ($stage -ge 1) { $gg.FillEllipse($brush, 8, [math]::Max(1,11-$h), 5, 4) }
            if ($stage -ge 2) { $gg.FillEllipse($darkBrush, 3, [math]::Max(4,12-$h), 5, 4) }
            if ($stage -ge 3) { $gg.FillEllipse($darkBrush, 9, [math]::Max(5,14-$h), 4, 3) }
        }.GetNewClosure()
    }

    $brush.Dispose(); $darkBrush.Dispose(); $greenBrush.Dispose()
    $whiteBrush.Dispose(); $grayBrush.Dispose(); $paperBrush.Dispose()

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

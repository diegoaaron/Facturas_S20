# Abre el informe en Word, regenera el índice y los campos, lo guarda y exporta una copia PDF de revisión.
# Uso: powershell -ExecutionPolicy Bypass -File actualizar_indice.ps1 [-Docx nombre.docx] [-Pdf ruta.pdf]
#      -Docx: archivo dentro de documentos_teoricos/entregable2 (por defecto, el informe completo).
param([string]$Docx = "entregable2_documento.docx", [string]$Pdf = "")
$docx = Join-Path (Split-Path (Split-Path $PSScriptRoot)) $Docx
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($docx)
    $doc.Fields.Update() | Out-Null
    foreach ($toc in $doc.TablesOfContents) { $toc.Update() }
    $doc.Save()
    if ($Pdf -ne "") { $doc.ExportAsFixedFormat($Pdf, 17) }
    Write-Output ("Páginas: " + $doc.ComputeStatistics(2))
    $doc.Close(0)
} finally {
    $word.Quit()
}

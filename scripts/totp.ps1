param([string] $Secret = "JBSWY3DPEHPK3PXP")

$normalized = $Secret.ToUpperInvariant().TrimEnd("=")
$alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
$bits = 0
$buffer = 0
$secretBytes = [System.Collections.Generic.List[byte]]::new()
foreach ($character in $normalized.ToCharArray()) {
    $value = $alphabet.IndexOf($character)
    if ($value -lt 0) { throw "TOTP secret must be Base32." }
    $buffer = ($buffer -shl 5) -bor $value
    $bits += 5
    if ($bits -ge 8) {
        $bits -= 8
        $secretBytes.Add([byte](($buffer -shr $bits) -band 255))
    }
}

$counter = [long][Math]::Floor(([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()) / 30)
$counterBytes = [byte[]]::new(8)
for ($index = 7; $index -ge 0; $index--) {
    $counterBytes[$index] = [byte]($counter -band 255)
    $counter = $counter -shr 8
}
$hmac = [System.Security.Cryptography.HMACSHA1]::new($secretBytes.ToArray())
try {
    $digest = $hmac.ComputeHash($counterBytes)
}
finally {
    $hmac.Dispose()
}
$offset = $digest[$digest.Length - 1] -band 15
$binary = (($digest[$offset] -band 127) -shl 24) -bor
    (($digest[$offset + 1] -band 255) -shl 16) -bor
    (($digest[$offset + 2] -band 255) -shl 8) -bor
    ($digest[$offset + 3] -band 255)
"{0:D6}" -f ($binary % 1000000)

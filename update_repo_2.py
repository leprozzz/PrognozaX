import re

with open('app/src/main/java/com/climateinsights/app/data/repository/WeatherRepository.kt', 'r') as f:
    content = f.read()

# Update isValidTemperature
content = re.sub(
    r'private fun isValidTemperature\(t: Double\): Boolean \{[\s\S]*?\}',
    '''private fun isValidTemperature(t: Double): Boolean {
        return t in -33.0..44.0
    }''',
    content
)

# Add sanitizeTemp if missing
if 'private fun sanitizeTemp' not in content:
    content = content.rstrip().rsplit('}', 1)[0] + '''
    private fun sanitizeTemp(temp: Double?): Double? {
        if (temp == null) return null
        return if (temp in -33.0..44.0) temp else null
    }
}'''

# Replace all toDoubleOrNull assignments
content = re.sub(r'tavg\s*=\s*(parts\[1\]\.trim\(\)\.toDoubleOrNull\(\))', r'tavg = sanitizeTemp(\1)', content)
content = re.sub(r'tmin\s*=\s*(parts\[2\]\.trim\(\)\.toDoubleOrNull\(\))', r'tmin = sanitizeTemp(\1)', content)
content = re.sub(r'tmax\s*=\s*(parts\[3\]\.trim\(\)\.toDoubleOrNull\(\))', r'tmax = sanitizeTemp(\1)', content)

content = re.sub(r'val tavg = (parts\[1\]\.trim\(\)\.toDoubleOrNull\(\))', r'val tavg = sanitizeTemp(\1)', content)
content = re.sub(r'val tmin = (parts\[2\]\.trim\(\)\.toDoubleOrNull\(\))', r'val tmin = sanitizeTemp(\1)', content)
content = re.sub(r'val tmax = (parts\[3\]\.trim\(\)\.toDoubleOrNull\(\))', r'val tmax = sanitizeTemp(\1)', content)

with open('app/src/main/java/com/climateinsights/app/data/repository/WeatherRepository.kt', 'w') as f:
    f.write(content)


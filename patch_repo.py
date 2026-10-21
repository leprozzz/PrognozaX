import re

with open('app/src/main/java/com/climateinsights/app/data/repository/WeatherRepository.kt', 'r') as f:
    content = f.read()

# Replace the parsing of temperatures in parseCsvFile
old_parse = '''                                tavg = parts[1].trim().toDoubleOrNull(),
                                tmin = parts[2].trim().toDoubleOrNull(),
                                tmax = parts[3].trim().toDoubleOrNull(),'''
new_parse = '''                                tavg = sanitizeTemp(parts[1].trim().toDoubleOrNull()),
                                tmin = sanitizeTemp(parts[2].trim().toDoubleOrNull()),
                                tmax = sanitizeTemp(parts[3].trim().toDoubleOrNull()),'''

if old_parse in content:
    content = content.replace(old_parse, new_parse)
    
    # Add sanitizeTemp function at the bottom
    sanitize_func = '''
    private fun sanitizeTemp(temp: Double?): Double? {
        if (temp == null) return null
        return if (temp in -33.0..44.0) temp else null
    }
}'''
    content = content.rstrip().rsplit('}', 1)[0] + sanitize_func
    
    with open('app/src/main/java/com/climateinsights/app/data/repository/WeatherRepository.kt', 'w') as f:
        f.write(content)
    print("Patched WeatherRepository successfully.")
else:
    print("Could not find old_parse.")

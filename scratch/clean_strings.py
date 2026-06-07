import os
import re

keys_to_remove = [
    "preference_search_files",
    "preference_search_files_summary",
    "preference_search_contacts",
    "preference_search_contacts_summary",
    "preference_search_calendar",
    "preference_search_calendar_summary",
    "preference_search_local_calendar_summary",
    "preference_search_tasks",
    "preference_search_tasks_summary",
    "preference_search_calculator",
    "preference_search_calculator_summary",
    "preference_search_unitconverter",
    "preference_search_unitconverter_summary",
    "preference_search_currencyconverter",
    "preference_search_currencyconverter_summary",
    "preference_search_supportedunits",
    "preference_search_websites",
    "preference_search_websites_summary",
    "preference_search_localfiles",
    "preference_search_localfiles_summary",
    "preference_search_nextcloud",
    "preference_search_cloud_summary",
    "preference_search_owncloud",
    "preference_calendar_calendars",
    "preference_calendar_hide_completed",
    "preference_search_osm_locations",
    "preference_search_osm_locations_summary",
    "plugin_type_calendar",
    "plugin_type_contacts",
    "plugin_type_locationsearch",
    "preference_search_wikipedia",
    "preference_search_wikipedia_summary"
]

def clean_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    modified = False
    for key in keys_to_remove:
        # Match <string name="key">...</string> or <plurals name="key">...</plurals>
        pattern = rf'<(string|plurals) name="{key}"(?:>.*?</\1>| />)'
        new_content = re.sub(pattern, '', content, flags=re.DOTALL)
        if new_content != content:
            content = new_content
            modified = True
    
    if modified:
        # Clean up empty lines left behind (optional but nice)
        content = re.sub(r'\n\s*\n', '\n', content)
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Cleaned {filepath}")

root_dir = "core/i18n/src/main/res"
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file == "strings.xml":
            clean_file(os.path.join(root, file))

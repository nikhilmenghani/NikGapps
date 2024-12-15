case "$1" in
 pre-backup)
   if [ "$execute_config" = "0" ]; then
     deleted=false
     for dir in "$S/addon.d/" "$T/addon.d/"; do
       for file in "$dir"*"$package_title.sh"; do
         if [ -f "$file" ]; then
           if [ "$deleted" = false ]; then
             ui_print "- Deleting $(basename $file)"
             deleted=true
           fi
           rm -f "$file"
         else
           addToLog "- $file does not exist."
         fi
       done
     done
     exit 1
   fi
 ;;
 backup)
   if [ "$execute_config" = "1" ]; then
     ui_print "- Backing up $package_title"
     list_files | while read FILE DUMMY; do
       backup_file $S/"$FILE"
     done
   fi
 ;;
 restore)
   if [ "$execute_config" = "1" ]; then
     addToLog " "
     ui_print "- Restoring $package_title"
     delete_in_system "$(delete_folders)" "Deleting aosp app"
     delete_in_system "$(force_delete_folders)" "Force Deleting"
     delete_in_system "$(debloat_folders)" "Debloating"
     delete_in_system "$(force_debloat_folders)" "Force Debloating"
     list_files | while read FILE REPLACEMENT; do
       R=""
       [ -n "$REPLACEMENT" ] && R="$S/$REPLACEMENT"
       [ -f "$C/$S/$FILE" ] && restore_file $S/"$FILE" "$R" && addFileToLog "$S/$FILE"
     done
     for i in $(list_files); do
       f=$(get_output_path "$S/$i")
       chown root:root "$f"
       chmod 644 "$f"
       chmod 755 $(dirname $f)
     done
     if list_build_props | grep -q '.'; then
       restore_build_props
     fi
   fi
 ;;
esac

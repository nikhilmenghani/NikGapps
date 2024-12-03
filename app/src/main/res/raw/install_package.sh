log_message() {
  echo "$1" >> "/sdcard/NikGapps/NikGapps_logs.log"
}

get_derived_path() {
    eval derived_path=\$${1}_derived_path
    echo $derived_path
}

RemoveAospAppsFromRom() {
    folders_that_exists=""
    for i in $(find "$system_derived_path" "$product_derived_path" "$system_ext_derived_path" -iname "$1" 2>/dev/null;); do
        if [ -d "$i" ]; then
            folders_that_exists="$folders_that_exists":"$i"
        fi
    done
    echo "$folders_that_exists"

}

set_perm() {
  chown "$1:$2" "$4"
  chmod "$3" "$4"
}

ch_con() {
  chcon -h u:object_r:"${1}"_file:s0 "$2"
}

install_file(){
  blank=""
  file_location=$(echo "$1" | sed "s/___/$blank/" | sed "s/___/\//g")
  source_location="$source_directory/$1"
  destination_location="$install_partition/$file_location"
  if [ -f "$source_location" ]; then
    mkdir -p "$(dirname "$destination_location")"
    set_perm 0 0 0755 "$(dirname "$destination_location")"
    log_message "- Copying $source_location to $destination_location"
    cp -f "$source_location" "$destination_location"
    ch_con system "$destination_location"
    set_perm 0 0 0644 "$destination_location"
  else
    log_message "- File $source_location not found"
    echo "File $source_location not found"
  fi
}

install_package(){
  for k in $remove_aosp_apps_from_rom; do
      folders_to_delete="$(RemoveAospAppsFromRom "$k")"
      OLD_IFS=$IFS
      IFS=":"
      for i in $folders_to_delete; do
          if [ -n "$i" ]; then
              echo "- Deleting "$i""
              log_message "- Deleting $i"
              rm -rf "$i"
          fi
      done
      IFS="$OLD_IFS"
  done

  install_partition=$(get_derived_path $default_partition)

  for i in $file_list; do
    install_file "$i"
  done
}

install_package

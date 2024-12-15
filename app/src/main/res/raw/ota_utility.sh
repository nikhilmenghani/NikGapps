#!/system/bin/sh

generate_filename() {
  directory="$1"; prefix="$2"; app_name="$3"
  max_index=0
  for file in "$directory"/"$prefix"-[0-9]*-"$app_name".sh; do
    [ -e "$file" ] || continue
    index=$(basename "$file" | cut -d '-' -f 2 | sed 's/[^0-9]*//g')
    if [ "$index" -eq "$index" ] 2>/dev/null; then
      echo "$file"
      return
    fi
  done
  for file in "$directory"/*.sh; do
    if grep -q "AFZC" "$file"; then
      file_name=$(basename "$file")
      file_constant=$(echo "$file_name" | cut -d '-' -f 1)
      if [ "$file_constant" = "$prefix" ]; then
        index=$(echo "$file_name" | cut -d '-' -f 2 | sed 's/[^0-9]*//g')
        if [ -n "$index" ] && [ "$index" -eq "$index" ] 2>/dev/null; then
          [ "$index" -gt "$max_index" ] && max_index="$index"
        fi
      fi
    fi
  done
  max_index="${max_index#0}"
  max_index=$((max_index + 1))
  new_file="$prefix-$(printf "%02d" "$max_index")-$app_name.sh"
  echo "$new_file"
}

set_perm() {
  chown "$1:$2" "$4"
  chmod "$3" "$4"
}

copy_ota_script() {
  if [ -f "$1" ]; then
    file_name=$(basename "$1")
    mkdir -p /system/addon.d
    cp "$1" "/system/addon.d/$file_name"
    set_perm 0 0 0644 "/system/addon.d/$file_name"
    [ -f "/system/addon.d/$file_name" ] && echo "Addon script copied successfully" || echo "Addon script not copied"
  else
    echo "Addon script not found"
  fi
}

read_prop() {
  type="$1"
  package_title="$2"
  installSource="/system/etc/permissions/$package_title.prop"
  if [ -f "$installSource" ]; then
    OLD_IFS="$IFS"
    IFS="$(printf '%b_' ' \n')"
    IFS="${IFS%_}"
    g=$(grep "$type=" "$installSource" | cut -d= -f2)
    for i in $g; do
      if [ -f "/system/$i" ]; then
        echo "$i"
      fi
    done
    IFS="$OLD_IFS"
  fi
}

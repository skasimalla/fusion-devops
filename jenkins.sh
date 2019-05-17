mkdir -p jsons
echo "{\"secret.1.password\":\"${Password_If_JDBC}\"}" > passwords.json

for i in $(echo $Name_Of_Fusion_Object | sed "s/,/ /g")
do
    echo "Performing $i"
	curl -k -u admin:xxx -o jsons/${i}.json "http://server:8764/api/apollo/objects/export?${Type_Of_Fusion_Object}.ids=${i}"
	curl -k -u admin:xxx\!xxx -H "Content-Type:multipart/form-data" -X POST -F "importData=@./jsons/${i}.json" -F "variableValues=@./passwords.json" "http://server:8764/api/apollo/objects/import?importPolicy=merge"

done
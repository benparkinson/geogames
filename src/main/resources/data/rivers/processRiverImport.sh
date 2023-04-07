#!/bin/bash

INPUTFILE=$1
OUTPUTFILE=$2.geo.json

tail -n +7 $INPUTFILE | head -n -2 | sed '/"@id":\|"alt_name:ku":\|"name:\|"id":\|"source:name\|"wikidata":\|"wikipedia":\|"alt_name":\|"alt_name:\|"destination:lang\|"gvr:code":\|"int_name":\|"is_in:country_code":\|"wikipedia:\|"code":\|"seasonal":\|"short_name:\|"order:\|"distance":/d' > $OUTPUTFILE

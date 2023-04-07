#!/bin/bash

INPUTFILE=$1
OUTPUTFILE=$2

tail -n +7 $INPUTFILE | head -n -2 | sed '/"@id":\|"alt_name:ku":\|"name:\|"id":\|"source:name\|"wikidata":\|"wikipedia":\|"alt_name":\|"alt_name:\|"destination:lang\|"gvr:code":\|"int_name":\|"is_in:country_code":\|"wikipedia:\|"code":/d' > $OUTPUTFILE

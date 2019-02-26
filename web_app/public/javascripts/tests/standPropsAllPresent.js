/**
Author: Aaron Yerke
Purpose: Test if all of the properties' IDs and names are present in the config file generator forms.
To use:
  1. cut and paste standard properties file into standPropsString.
  2. cut and paste wiki table into convertPropertiesWikiToMap()
  3. start webapp with npm start
  4. in config menu type missingElementsWithDescr() in javascript console.

  NOTE: Checklist options dont get IDs.
*/

const standPropsString = `INSERT STANDARD PROPS HERE`;

function findMissingProps(){
  const standPropsLines = standPropsString.split('\n');
  let missingPropsId = new Set();
  let missingPropsIdAndName = new Set();

  for (var i = 0; i < standPropsLines.length; i++) {
    console.log(standPropsLines[i]);
    let line = standPropsLines[i];
    if (line.includes('=')){
      line = line.replace('#', '')
        .trim()
        .split('=')[0];
      console.log(line);
      if (!document.getElementById(line)){
        missingPropsId.add(line);
        if (!document.getElementsByName(line)){
          missingPropsIdAndName.add(line)
        }
      }
    }
  }//end forloop
  console.log('elements absent in ID search:', missingPropsId);
  console.log('elements absent in ID and name search', missingPropsIdAndName);
  return missingPropsId;
}//end findMissingProps

function convertPropertiesWikiToMap() {
  const wikiTable = `INSERT WIKITABLE HERE`
  const wikiTableArray = wikiTable.split('\n');
  let wikiTableMap = new Map();

  for (var i = 0; i < wikiTableArray.length; i++) {
    const split = wikiTableArray[i].split('\t')
    wikiTableMap.set(split[0], split[1])
  }
  return wikiTableMap;
}

function missingElementsWithDescr(){
  let missingElements = Array.from(findMissingProps());
  let wikiMap = convertPropertiesWikiToMap();

  for (let ele of wikiMap.keys()){
    if (!missingElements.includes(ele)){
      console.log(ele);
      wikiMap.delete(ele);
      console.log(wikiMap);
    }
  }
  return wikiMap;
}

function parseClassFromDocumentReturnIds(classNam = 'sp') {
  let localId = [];
  const sp = document.getElementsByClassName(classNam);
  for (var i = 0; i < sp.length; i++) {
    //console.log(sp[i]);
    localId.push(sp[i].id)
  }
  return localId;
}

function parseIdsFromStandPropString(standProps = standPropsString) {
  const standPropsLines = standProps.split('\n');
  let spId = [];

  for (var i = 0; i < standPropsLines.length; i++) {
    console.log(standPropsLines[i]);
    let line = standPropsLines[i];
    if (line.includes('=')){
      line = line.replace('#', '')
        .trim()
        .split('=')[0];
      //console.log(line);
      spId.push(line)
    }
  }
  return spId;
}

/**
//The following function usage in app.

let spClass = returnClassIds();
let stnPrps = returnStandPropIds()

//For elements missing from page:
console.log(inArray1ButNotArray2(stnPrps, spClass));

//For extra elements on the page:
console.log(inArray1ButNotArray2(spClass, stnPrps));
*/
function inArray1ButNotArray2(array1, array2) {
  let missing = [];
  for (var i = 0; i < array1.length; i++) {
    if (!array2.includes(array1[i])) {
      missing.push(array1[i])
    }
  }
  return missing;
}



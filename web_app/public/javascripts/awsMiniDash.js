/**
Author: Aaron Yerke
Purpose: Wrappers for the mjzapata/AWSBatchGenomicsStack repository on github.
Notes: Its a work in progress
*/


document.getElementById('getMalcolmGitRepo').addEventListener('click', function(evt){
  evt.preventDefault();
  sendFormToNode('getMalcolmGitRepo', '/getMalcolmGitRepo');
});
document.getElementById('validateAwsCreditials').addEventListener('click', function(evt){
  evt.preventDefault();
  setTimeout(function(){
    var request = new XMLHttpRequest();
    request.open('POST', '/validateAwsCreditials', true);
    request.setRequestHeader("Content-Type", "application/json");
    request.send();
    console.log('AWS VALIDATION REQUEST SENT');
    request.onreadystatechange = function() {
    if (request.readyState == XMLHttpRequest.DONE) {
      console.log(request.responseText);
      document.getElementById('awsCredentialsStatus').innerHTML = request.responseText;
      }
    }
    console.log(request.responseText);
  }, 2000);
});

document.getElementById('submitConfigureAWS').addEventListener('click', function(evt){
  evt.preventDefault();
  let thisForm = document.getElementById('configureAwsForm');
    if (thisForm.checkValidity()){
    console.log("this: ",this);
    let promis = sendFormToNode('submitConfigureAWS', '/configureAws');
      promis.then(result => {
        console.log('result: ', result);
        let correct = Array.from(thisForm.getElementsByClassName('correctInput'));
        let incorrect = Array.from(thisForm.getElementsByClassName('incorrectInput'));
        if (result.trim() === 'valid'){
          correct.forEach( ele => ele.style.display = 'inline');
          incorrect.forEach( ele => ele.style.display = 'none');
        }else {
          incorrect.forEach( ele => ele.style.display = 'inline');
          correct.forEach( ele => ele.style.display = 'none');
        }
      })
    }
});
//document.getElementById('configureAwsForm').getElementsByTagName('span').getElementsByClassName('correctInput')
document.getElementById('deployComputeStack').addEventListener('click', function(evt){
  evt.preventDefault();
  console.log(this);
  this.disabled = true;
  let correct = Array.from(this.parentNode.getElementsByClassName('correctInput'));
  let incorrect = Array.from(this.parentNode.getElementsByClassName('incorrectInput'));
  let inProgress = Array.from(this.parentNode.getElementsByClassName('inProgress'));
  incorrect.forEach( ele => ele.style.display = 'none');
  correct.forEach( ele => ele.style.display = 'none');
  inProgress.forEach( ele => ele.style.display = 'block');
  let formData = {};
  let myForm = new FormData(this.parentNode.parentNode);
  for (var i of myForm.entries()) {
    formData[i[0]] = i[1];
  }
  console.log('formData: ', formData);
  let request = new XMLHttpRequest();
  request.timeout = 1000 * 10 * 60;
  request.ontimeout = function (e) {
    console.log('timed out from browser \n', e);
  };
  request.open("POST", '/deployCloudInfrastructure', true);
  request.setRequestHeader("Content-Type", "application/json");
  request.send(JSON.stringify({formData}));
  request.onreadystatechange = function() {
    if (request.readyState == XMLHttpRequest.DONE) {
      console.log(request);
      console.log('request.responseText: ', request.responseText);
      const rt = request.responseText.trim();
      inProgress.forEach( ele => ele.style.display = 'none');
      if (rt.endsWith("_COMPLETE")){
        correct.forEach( ele => ele.style.display = 'inline');
      }else if (rt.endsWith("FAILED") ){
        incorrect.forEach( ele => ele.style.display = 'inline');
      }else {
        incorrect.forEach( ele => ele.style.display = 'inline');
        correct.forEach( ele => ele.style.display = 'none');
      }
    }
    console.log('undisabling');
    this.disabled = false;
  }
});

document.getElementById('launchEc2HeadNodeButton').addEventListener('click', function(evt) {
  evt.preventDefault();
  console.log('this.parentNode: ', this.parentNode);
  let correct = Array.from(this.parentNode.getElementsByClassName('correctInput'));
  let incorrect = Array.from(this.parentNode.getElementsByClassName('incorrectInput'));
  let inProgress = Array.from(this.parentNode.getElementsByClassName('inProgress'));
  incorrect.forEach( ele => ele.style.display = 'none');
  correct.forEach( ele => ele.style.display = 'none');
  inProgress.forEach( ele => ele.style.display = 'block');
  let formData = {};
  let myForm = new FormData(this.parentNode.parentNode);
  for (var i of myForm.entries()) {
    formData[i[0]] = i[1];
  }
  console.log('formData: ', formData);
  let request = new XMLHttpRequest();
  request.timeout = 1000 * 10 * 60;
  request.ontimeout = function (e) {
    console.log('timed out from browser \n', e);
  };
  request.open("POST", '/deployCloudInfrastructure', true);
  request.setRequestHeader("Content-Type", "application/json");
  request.send(JSON.stringify({formData}));
  request.onreadystatechange = function() {
    if (request.readyState == XMLHttpRequest.DONE) {
      console.log(request);
      console.log('request.responseText: ', request.responseText);
      const rt = request.responseText.trim();
      inProgress.forEach( ele => ele.style.display = 'none');
      if (rt.endsWith("_COMPLETE")){
        correct.forEach( ele => ele.style.display = 'inline');
      }else if (rt.endsWith("FAILED") ){
        incorrect.forEach( ele => ele.style.display = 'inline');
      }else {
        incorrect.forEach( ele => ele.style.display = 'inline');
        correct.forEach( ele => ele.style.display = 'none');
      }
    }
  }
});

//copy past from config_config
function sendFormToNode( formElementId, nodeAddress, requestMethod = 'POST', timeOut = 2) {
  //timeOut should be in minutes
  return new Promise((resolve, reject) => {
    let formData = {};
    let myForm = new FormData(document.getElementById(formElementId).parentNode.parentNode);
    for (var i of myForm.entries()) {
      formData[i[0]] = i[1];
    }
    console.log('formData: ', formData);

    let request = new XMLHttpRequest();
    request.open(requestMethod, nodeAddress, true);
    request.timeout = 1000 * 60 * timeOut;
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify({formData}));
    request.onreadystatechange = function() {
      if (request.readyState === XMLHttpRequest.DONE) {
        try {
          if(this.status === 200 && request.readyState === 4){
            console.log(this.responseText);
            resolve(this.responseText);
          }else{
            reject(this.status + " " + this.statusText)
          }
        } catch (e) {
          reject (e.message)
        }
      }
    }
  });
};

function retrieveStackListAsPromise() {
  try {
    return new Promise((resolve, reject) => {
      let request = new XMLHttpRequest();
      request.open('POST', '/retrieveAwsStackLists', true);
      request.setRequestHeader("Content-Type", "application/json");
      request.send();
      console.log('request sent');
      request.onreadystatechange = function() {
        if (request.readyState === XMLHttpRequest.DONE) {
          try {
            if(this.status === 200 && request.readyState === 4){
              console.log(this.responseText);
              resolve(this.responseText);
            }else{
              reject(this.status + " " + this.statusText)
            }
          } catch (e) {
            reject (e.message)
          }
        }
      }
    });
  } catch (e) {
    console.error(e);
  }
}
function updateCloudStackElements() {
  const awsMiniDash = document.getElementById('awsSettings');
  let datLis = Array.from(awsMiniDash.getElementsByClassName('stackList'));
  console.log('datLis: ', datLis);
  let stacks = retrieveStackListAsPromise();
  stacks.then(dat => {
    console.log(dat);
    let data = dat.trim().split('\n');
    console.log(data);
    datLis.forEach( ele => {

      //remove child nodes
      while (ele.firstChild) {
        ele.removeChild(ele.firstChild);
      }

      //iterate through stackname list (data)
      for (var i = 0; i < data.length; i++) {
        let o = document.createElement('option');
        o.innerHTML = data[i];
        o.value = data[i];
        ele.appendChild(o);
        console.log('ele: ', ele);
      }
    })
  })//.then end

  console.log();
}

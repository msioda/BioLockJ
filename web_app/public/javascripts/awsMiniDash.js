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
  console.log('this.parentNode: ', this.parentNode);
  const targ = this;
  let inProgress = Array.from(this.parentNode.getElementsByClassName('inProgress'));
  inProgress.forEach( ele => ele.style.display = 'block');
  console.log('this deployComputeStack: ', this);
  let formData = {};
  let myForm = new FormData(this.parentNode.parentNode);
  for (var i of myForm.entries()) {
    formData[i[0]] = i[1];
  }
  console.log('formData: ', formData);
  let request = new XMLHttpRequest();
  request.open("POST", '/deployCloudInfrastructure', true);
  request.setRequestHeader("Content-Type", "application/json");
  request.send(JSON.stringify({formData}));
  request.onreadystatechange = function() {
    if (request.readyState == XMLHttpRequest.DONE) {
      console.log(request);
      console.log('request.responseText: ', request.responseText);
      console.log('targ: ', targ);
      const rt = request.responseText.trim();
      inProgress.forEach( ele => ele.style.display = 'none');
      let correct = Array.from(targ.parentNode.getElementsByClassName('correctInput'));
      let incorrect = Array.from(targ.parentNode.getElementsByClassName('incorrectInput'));
      if (rt.endsWith("_COMPLETE")){
        correct.forEach( ele => ele.style.display = 'inline');
        incorrect.forEach( ele => ele.style.display = 'none');
      } if (rt.endsWith("FAILED") ){
        incorrect.forEach( ele => ele.style.display = 'inline');
        correct.forEach( ele => ele.style.display = 'none');
      }else {
        incorrect.forEach( ele => ele.style.display = 'inline');
        correct.forEach( ele => ele.style.display = 'none');
      }
    }
  }
});
document.getElementById('launchEc2HeadNodeButton').addEventListener('click', function(evt) {
  evt.preventDefault();
  sendFormToNode('launchEc2HeadNodeButton', 'launchEc2HeadNode')
});
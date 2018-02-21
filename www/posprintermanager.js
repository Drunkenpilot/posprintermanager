var exec = require('cordova/exec');

exports.buildImage = function(arg0, success, error) {
    exec(success, error, "posprintermanager", "buildImage", [arg0]);
};
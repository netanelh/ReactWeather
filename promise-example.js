// function getTempCallback(location, callback) {
//     callback(undefined, 79);
//     callback('could not find location');
// }
//
// getTempCallback('Ashdod', function (error, temp) {
//     if (error) {
//         console.log('Error -', error);
//     } else {
//         console.log('success', temp);
//     }
// });
//
// function getTempPromise(location) {
//     return new Promise(function (resolve, reject) {
//         setTimeout(function () {
//             resolve(79);
//             reject('Promise error');
//         }, 1000);
//     });
// }
//
// getTempPromise('Ashdod').then(function (temp) {
//     console.log('promise success', temp);
// }, function (error) {
//     console.log('promise error');
// });


function addPromise(a, b) {
    return new Promise(function (resolve, reject) {
        if ((typeof a === 'number') && (typeof b === 'number')) {
            resolve(a + b);
        } else {
            reject('Both numbers are not numbers');
        }
    });
}

addPromise(7, 9).then(function (result) {
    console.log('result is:', result);
}, function (error) {
    console.log('error -', error);
});
addPromise(7, '9').then(function (result) {
    console.log('result is:', result);
}, function (error) {
    console.log('error -', error);
});

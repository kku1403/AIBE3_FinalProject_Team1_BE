const fs = require('fs');
const archiver = require('archiver');

console.log('==========================================');
console.log('Lambda 함수 패키지 생성 (Node.js)');
console.log('==========================================');

// 명령행 인자로 타입 지정 (profile 또는 post)
const type = process.argv[2] || 'profile';

const configs = {
    profile: {
        sourceFile: 'index.js',
        outputFile: 'profile_resizer.zip'
    },
    post: {
        sourceFile: 'post-image-resizer.js',
        outputFile: 'post_resizer.zip'
    }
};

const config = configs[type];

if (!config) {
    console.error(`❌ 잘못된 타입: ${type}`);
    console.error('사용법: node create-zip.js [profile|post]');
    process.exit(1);
}

console.log(`\n타입: ${type}`);
console.log(`소스 파일: ${config.sourceFile}`);
console.log(`출력 파일: ${config.outputFile}\n`);

// 소스 파일 존재 확인
if (!fs.existsSync(config.sourceFile)) {
    console.error(`❌ 파일이 없습니다: ${config.sourceFile}`);
    process.exit(1);
}

const output = fs.createWriteStream(config.outputFile);
const archive = archiver('zip', {
    zlib: { level: 9 }
});

output.on('close', function() {
    console.log('');
    console.log('==========================================');
    console.log('✅ Lambda 패키지 생성 완료!');
    console.log('==========================================');
    console.log('');
    console.log(`파일 크기: ${(archive.pointer() / 1024 / 1024).toFixed(2)} MB`);
    console.log(`파일 위치: ${config.outputFile}`);
    console.log('');
    console.log('==========================================');
    console.log('다음 단계:');
    console.log('1. cd ..');
    console.log('2. terraform plan');
    console.log('3. terraform apply');
    console.log('==========================================');
});

archive.on('error', function(err) {
    throw err;
});

archive.pipe(output);

console.log('📦 파일 압축 중...');

// Lambda 함수 파일 추가
archive.file(config.sourceFile, {
    name: type === 'profile' ? 'index.js' : 'post-image-resizer.js'
});

// node_modules 폴더 추가
archive.directory('node_modules/', 'node_modules');

archive.finalize();
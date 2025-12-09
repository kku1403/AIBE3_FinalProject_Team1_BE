const sharp = require('sharp');
const { S3Client, GetObjectCommand, PutObjectCommand } = require("@aws-sdk/client-s3");

const s3 = new S3Client({ region: "ap-northeast-2" });

// 환경변수
const BUCKET_NAME = process.env.BUCKET_NAME;
const SOURCE_PREFIX = process.env.SOURCE_PREFIX || 'posts/images/originals/';
const DESTINATION_PREFIX = process.env.DESTINATION_PREFIX || 'posts/images/resized/';

// 게시글 이미지 크기 설정
const SIZES = {
    thumbnail: { width: 800, height: 600 },   // 목록용 4:3
    detail: { width: 1920, height: 1440 }     // 상세용 4:3
};

const QUALITY = 85;

function streamToBuffer(stream) {
    return new Promise((resolve, reject) => {
        const chunks = [];
        stream.on("data", chunk => chunks.push(chunk));
        stream.on("end", () => resolve(Buffer.concat(chunks)));
        stream.on("error", reject);
    });
}

exports.handler = async (event) => {
    console.log('Event:', JSON.stringify(event, null, 2));

    // S3 이벤트에서 정보 추출
    const bucket = event.Records[0].s3.bucket.name;
    const key = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, ' '));

    console.log(`Event received - Bucket: ${bucket}, Key: ${key}`);

    // 버킷 검증
    if (bucket !== BUCKET_NAME) {
        console.log(`❌ Skip: Wrong bucket (expected: ${BUCKET_NAME})`);
        return { statusCode: 200, body: 'Skipped: wrong bucket' };
    }

    // 경로 검증
    if (!key.startsWith(SOURCE_PREFIX)) {
        console.log(`❌ Skip: Wrong path (expected: ${SOURCE_PREFIX})`);
        return { statusCode: 200, body: 'Skipped: wrong path' };
    }

    try {
        // 원본 이미지 다운로드
        console.log(`📥 Downloading: ${key}`);
        const originalImage = await s3.send(
            new GetObjectCommand({
                Bucket: bucket,
                Key: key
            })
        );

        const imageBuffer = await streamToBuffer(originalImage.Body);

        // 파일명 추출
        const filename = key.split('/').pop();
        const nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));

        const results = [];

        // 각 크기별 리사이징 및 업로드
        for (const [sizeName, dimensions] of Object.entries(SIZES)) {
            console.log(`🖼️  Resizing to ${sizeName}: ${dimensions.width}x${dimensions.height}...`);

            const resizedImage = await sharp(imageBuffer)
                .resize(dimensions.width, dimensions.height, {
                    fit: 'cover',           // 4:3 비율 유지하며 크롭
                    position: 'centre'
                })
                .webp({
                    quality: QUALITY,
                    effort: 6
                })
                .toBuffer();

            // 대상 key 생성
            const destinationKey = `${DESTINATION_PREFIX}${sizeName}/${nameWithoutExt}.webp`;

            // S3 업로드
            console.log(`📤 Uploading: ${destinationKey}`);
            await s3.send(
                new PutObjectCommand({
                    Bucket: bucket,
                    Key: destinationKey,
                    Body: resizedImage,
                    ContentType: "image/webp",
                    CacheControl: "max-age=31536000"
                })
            );

            results.push({
                size: sizeName,
                key: destinationKey,
                dimensions: `${dimensions.width}x${dimensions.height}`,
                bytes: resizedImage.length
            });

            console.log(`✅ ${sizeName}: ${destinationKey} (${resizedImage.length} bytes)`);
        }

        console.log(`✅ All sizes completed for: ${key}`);

        return {
            statusCode: 200,
            body: JSON.stringify({
                original: key,
                originalSize: imageBuffer.length,
                results: results
            })
        };

    } catch (error) {
        console.error(`❌ Error: ${error.message}`);
        console.error(error.stack);

        return {
            statusCode: 200,
            body: JSON.stringify({
                error: error.message
            })
        };
    }
};